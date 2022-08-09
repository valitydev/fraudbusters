package dev.vality.fraudbusters;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.constants.LoadDataIntegrationsTemplates;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.vality.fraudbusters.util.BeanUtil.createChargeback;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ActiveProfiles("full-prod")
@KafkaTestcontainer(topicsKeys = {
        "kafka.topic.template",
        "kafka.topic.reference",
        "kafka.topic.event.sink.payment"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "kafka.listen.result.concurrency=1",
        "kafka.historical.listener.enable=true",
        "kafka.aggr.payment.min.bytes=1"},
        classes = TestClickhouseConfig.class)
@Import({MockExternalServiceConfig.class, KafkaProducerConfig.class})
class LoadDataIntegrationTest {

    public static final String PAYMENT_1 = "payment_1";
    public static final String PAYMENT_2 = "payment_2";
    public static final String PAYMENT_0 = "payment_0";

    public static final int DEFAULT_TIMEOUT = 15;

    private final String globalRef = UUID.randomUUID().toString();

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private KafkaTopics kafkaTopics;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
    @Autowired
    private ColumbusServiceSrv.Iface geoIpServiceSrv;
    @Autowired
    private HistoricalPool<ParserRuleContext> timeTemplateTimePoolImpl;
    @Autowired
    @Qualifier("timeReferencePoolImpl")
    private HistoricalPool<String> timeReferencePoolImpl;

    @LocalServerPort
    int serverPort;

    @Test
    @SneakyThrows
    void testLoadData() {
        when(geoIpServiceSrv.getLocationIsoCode(any())).thenReturn("RUS");
        Command crateCommandTemplate =
                TestObjectsFactory.createCommandTemplate(globalRef, LoadDataIntegrationsTemplates.TEMPLATE);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandTemplate);
        Command crateCommandReference = TestObjectsFactory.crateCommandReference(globalRef);
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(), crateCommandReference);

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() ->
                timeTemplateTimePoolImpl.size() == 1);
        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() ->
                timeReferencePoolImpl.size() == 1);

        final String oldTime = String.valueOf(LocalDateTime.now());
        Command crateCommandTemplate2 =
                TestObjectsFactory.createCommandTemplate(globalRef, LoadDataIntegrationsTemplates.TEMPLATE_2);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandTemplate2);

        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format("http://localhost:%s/fraud_payment_validator/v1/", serverPort)))
                .withNetworkTimeout(300000);
        PaymentServiceSrv.Iface client = clientBuilder.build(PaymentServiceSrv.Iface.class);

        checkInsertingBatch(client);

        Payment payment = BeanUtil.createPayment(PaymentStatus.processed);
        payment.setId(PAYMENT_1);
        insert(client, List.of(payment));
        insertListDefaultPayments(client, PaymentStatus.captured, PaymentStatus.failed);
        checkPayment(PAYMENT_1, ResultStatus.DECLINE, 1);

        //check in past
        payment.setId(PAYMENT_0);
        payment.setEventTime(oldTime);
        insert(client, payment);
        checkPayment(PAYMENT_0, ResultStatus.THREE_DS, 1);

        String localId = UUID.randomUUID().toString();
        Command crateCommandConcreteTemplate =
                TestObjectsFactory.createCommandTemplate(localId, LoadDataIntegrationsTemplates.TEMPLATE_CONCRETE);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandConcreteTemplate);
        Command crateCommandConcreteReference = TestObjectsFactory.crateCommandReference(localId);
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(), crateCommandConcreteReference);

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> timeTemplateTimePoolImpl.get(localId, 0L) == null);
        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> timeReferencePoolImpl.get(localId, 0L) == null);

        payment.setId(PAYMENT_2);
        payment.setEventTime(String.valueOf(LocalDateTime.now()));
        insert(client, payment);
        checkPayment(PAYMENT_2, ResultStatus.ACCEPT, 1);

        //Chargeback
        client.insertChargebacks(List.of(
                createChargeback(dev.vality.damsel.fraudbusters.ChargebackStatus.accepted),
                createChargeback(dev.vality.damsel.fraudbusters.ChargebackStatus.cancelled)
        ));

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_CHARGEBACK.getTable()).size() == 2);

        //Refund
        client.insertRefunds(List.of(
                BeanUtil.createRefund(dev.vality.damsel.fraudbusters.RefundStatus.succeeded),
                BeanUtil.createRefund(dev.vality.damsel.fraudbusters.RefundStatus.failed)
        ));

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_REFUND.getTable()).size() == 2);

        //Withdrawal
        client.insertWithdrawals(List.of(
                createChargeback(WithdrawalStatus.pending),
                createChargeback(WithdrawalStatus.failed),
                createChargeback(WithdrawalStatus.succeeded)
        ));

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_WITHDRAWAL.getTable()).size() == 3);
    }

    private void checkInsertingBatch(PaymentServiceSrv.Iface client) throws TException, InterruptedException {
        insert(
                client,
                List.of(BeanUtil.createPayment(PaymentStatus.processed, "1"),
                        BeanUtil.createPayment(PaymentStatus.processed, "2"),
                        BeanUtil.createPayment(PaymentStatus.processed, "3"),
                        BeanUtil.createPayment(PaymentStatus.processed, "4"),
                        BeanUtil.createPayment(PaymentStatus.processed, "5")
                )
        );

        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_PAYMENT.getTable()).size() == 5);
    }

    private void insert(PaymentServiceSrv.Iface client, Payment payment)
            throws TException, InterruptedException {
        insert(client, List.of(payment));
    }

    private void insert(PaymentServiceSrv.Iface client, List<Payment> payments)
            throws TException, InterruptedException {
        client.insertPayments(payments);
    }

    private void checkPayment(String payment1, ResultStatus status, int expectedCount) {
        await().atMost(DEFAULT_TIMEOUT, SECONDS).until(() -> {
            List<Map<String, Object>> maps =
                    jdbcTemplate.queryForList(String.format("SELECT * from fraud.payment where id='%s'", payment1));
            return maps.size() == expectedCount && maps.get(0).get("resultStatus").equals(status.name());
        });
    }

    private void insertListDefaultPayments(
            PaymentServiceSrv.Iface client,
            PaymentStatus processed,
            PaymentStatus processed2) throws TException, InterruptedException {
        insert(client, List.of(BeanUtil.createPayment(processed), BeanUtil.createPayment(processed2)));
    }

}
