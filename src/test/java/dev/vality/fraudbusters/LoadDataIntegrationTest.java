package dev.vality.fraudbusters;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.testcontainers.annotations.clickhouse.ClickhouseTestcontainer;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.vality.fraudbusters.util.BeanUtil.createChargeback;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("full-prod")
@KafkaTestcontainer(
        properties = {
                "kafka.listen.result.concurrency=1",
                "kafka.historical.listener.enable=true",
                "kafka.aggr.payment.min.bytes=1"},
        topicsKeys = {
                "kafka.topic.template",
                "kafka.topic.reference",
                "kafka.topic.event.sink.payment"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(
        classes = {KafkaProducerConfig.class}
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ClickhouseTestcontainer(migrations = {
        "sql/db_init.sql",
        "sql/V3__create_fraud_payments.sql",
        "sql/V4__create_payment.sql",
        "sql/V5__add_fields.sql",
        "sql/V6__add_result_fields_payment.sql",
        "sql/V7__add_fields.sql",
        "sql/V8__create_withdrawal.sql",
        "sql/V9__add_phone_category_card.sql"})
@Import(MockExternalServiceConfig.class)
class LoadDataIntegrationTest {

    public static final String PAYMENT_1 = "payment_1";
    public static final String PAYMENT_2 = "payment_2";
    public static final String PAYMENT_0 = "payment_0";

    private static final String TEMPLATE = """
            rule:TEMPLATE: sum("card_token", 1000, "party_id", "shop_id", "mobile") > 0
             and unique("email", "ip", 1444, "recurrent") < 2 and isRecurrent() == false
             and count("card_token", 1000, "party_id", "shop_id") > 5  -> decline
            """;
    private static final String TEMPLATE_2 =
            "rule:TEMPLATE: count(\"card_token\", 1000, \"party_id\", \"shop_id\") > 2  -> decline;";
    private static final String TEMPLATE_CONCRETE =
            "rule:TEMPLATE_CONCRETE: count(\"card_token\", 10) > 0  -> accept;";
    private final String globalRef = UUID.randomUUID().toString();
    private static final long TIMEOUT = 1000L;

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
        Command crateCommandTemplate = TestObjectsFactory.crateCommandTemplate(globalRef, TEMPLATE);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandTemplate);
        Command crateCommandReference = TestObjectsFactory.crateCommandReference(globalRef);
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(), crateCommandReference);

        await().until(() ->
                timeTemplateTimePoolImpl.size() == 1);
        await().until(() ->
                timeReferencePoolImpl.size() == 1);

        final String oldTime = String.valueOf(LocalDateTime.now());
        Command crateCommandTemplate2 = TestObjectsFactory.crateCommandTemplate(globalRef, TEMPLATE_2);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandTemplate2);

        await().until(() ->
                timeTemplateTimePoolImpl.size() == 2);

        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format("http://localhost:%s/fraud_payment_validator/v1/", serverPort)))
                .withNetworkTimeout(300000);
        PaymentServiceSrv.Iface client = clientBuilder.build(PaymentServiceSrv.Iface.class);

        checkInsertingBatch(client);

        Payment payment = BeanUtil.createPayment(PaymentStatus.processed);
        payment.setId(PAYMENT_1);
        insertWithTimeout(client, List.of(payment));
        insertListDefaultPayments(client, PaymentStatus.captured, PaymentStatus.failed);
        checkPayment(PAYMENT_1, ResultStatus.DECLINE, 1);

        //check in past
        payment.setId(PAYMENT_0);
        payment.setEventTime(oldTime);
        insertWithTimeout(client, payment);
        checkPayment(PAYMENT_0, ResultStatus.THREE_DS, 1);

        String localId = UUID.randomUUID().toString();
        Command crateCommandConcreteTemplate = TestObjectsFactory.crateCommandTemplate(localId, TEMPLATE_CONCRETE);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandConcreteTemplate);
        Command crateCommandConcreteReference = TestObjectsFactory.crateCommandReference(localId);
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(), crateCommandConcreteReference);

        await().until(() -> timeTemplateTimePoolImpl.size() == 3);
        await().until(() -> timeReferencePoolImpl.size() == 2);

        payment.setId(PAYMENT_2);
        payment.setEventTime(String.valueOf(LocalDateTime.now()));
        insertWithTimeout(client, payment);
        checkPayment(PAYMENT_2, ResultStatus.ACCEPT, 1);

        //Chargeback
        client.insertChargebacks(List.of(
                createChargeback(dev.vality.damsel.fraudbusters.ChargebackStatus.accepted),
                createChargeback(dev.vality.damsel.fraudbusters.ChargebackStatus.cancelled)
        ));
//        Thread.sleep(TIMEOUT);

        await().until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_CHARGEBACK.getTable()).size() == 2);

//        List<Map<String, Object>> maps =
//                jdbcTemplate.queryForList("SELECT * from " + EventSource.FRAUD_EVENTS_CHARGEBACK.getTable());
//        assertEquals(2, maps.size());

        //Refund
        client.insertRefunds(List.of(
                BeanUtil.createRefund(dev.vality.damsel.fraudbusters.RefundStatus.succeeded),
                BeanUtil.createRefund(dev.vality.damsel.fraudbusters.RefundStatus.failed)
        ));
//        Thread.sleep(TIMEOUT);

        await().until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_REFUND.getTable()).size() == 2);
//        maps = jdbcTemplate.queryForList("SELECT * from " + EventSource.FRAUD_EVENTS_REFUND.getTable());
//        assertEquals(2, maps.size());

        //Withdrawal
        client.insertWithdrawals(List.of(
                createChargeback(WithdrawalStatus.pending),
                createChargeback(WithdrawalStatus.failed),
                createChargeback(WithdrawalStatus.succeeded)
        ));

//        Thread.sleep(TIMEOUT);

        await().until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_WITHDRAWAL.getTable()).size() == 3);
//        maps = jdbcTemplate.queryForList("SELECT * from " + EventSource.FRAUD_EVENTS_WITHDRAWAL.getTable());
//        assertEquals(3, maps.size());
    }

    private void checkInsertingBatch(PaymentServiceSrv.Iface client) throws TException, InterruptedException {
        insertWithTimeout(
                client,
                List.of(BeanUtil.createPayment(PaymentStatus.processed),
                        BeanUtil.createPayment(PaymentStatus.processed),
                        BeanUtil.createPayment(PaymentStatus.processed),
                        BeanUtil.createPayment(PaymentStatus.processed),
                        BeanUtil.createPayment(PaymentStatus.processed)
                )
        );

        await().until(() -> jdbcTemplate.queryForList("SELECT * from " +
                EventSource.FRAUD_EVENTS_PAYMENT.getTable()).size() == 5);

//        List<Map<String, Object>> maps =
//                jdbcTemplate.queryForList("SELECT * from " + EventSource.FRAUD_EVENTS_PAYMENT.getTable());
//        assertEquals(5, maps.size());
//        assertEquals("email", maps.get(0).get("email"));
//        Thread.sleep(TIMEOUT);
    }

    private void insertWithTimeout(PaymentServiceSrv.Iface client, Payment payment)
            throws TException, InterruptedException {
        insertWithTimeout(client, List.of(payment));
    }

    private void insertWithTimeout(PaymentServiceSrv.Iface client, List<Payment> payments)
            throws TException, InterruptedException {
        client.insertPayments(payments);
        Thread.sleep(TIMEOUT * 5);
    }

    private void checkPayment(String payment1, ResultStatus status, int expectedCount) {
        List<Map<String, Object>> maps =
                jdbcTemplate.queryForList(String.format("SELECT * from fraud.payment where id='%s'", payment1));
        log.info("SELECT : {}", maps);
        assertEquals(expectedCount, maps.size());
        assertEquals(status.name(), maps.get(0).get("resultStatus"));
    }

    private void insertListDefaultPayments(
            PaymentServiceSrv.Iface client,
            PaymentStatus processed,
            PaymentStatus processed2) throws TException, InterruptedException {
        insertWithTimeout(client, List.of(BeanUtil.createPayment(processed), BeanUtil.createPayment(processed2)));
    }

}
