package dev.vality.fraudbusters;

import dev.vality.damsel.fraudbusters.PaymentServiceSrv;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.FraudPaymentRepositoryTest;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ActiveProfiles("full-prod")
@KafkaSpringBootTest
@KafkaTestcontainer(properties = {"kafka.listen.result.concurrency=1"},
        topicsKeys = {"kafka.topic.event.sink.payment", "kafka.topic.fraud.payment"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith({ClickHouseContainerExtension.class})
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.main.allow-bean-definition-overriding=true"},
        classes = TestClickhouseConfig.class)
@Import({MockExternalServiceConfig.class})
class FraudPaymentTest {

    public static final String ID_PAYMENT = "inv";
    public static final String EMAIL = "kek@kek.ru";
    public static final String SELECT_FROM_FRAUD_FRAUD_PAYMENT = "SELECT * from fraud.fraud_payment";
    public static final String SELECT_FROM_PAYMENT = "SELECT * from fraud.payment";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int serverPort;

    @SneakyThrows
    @Test
    public void testFraudPayment() {
        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format("http://localhost:%s/fraud_payment/v1/", serverPort)))
                .withNetworkTimeout(300000);
        PaymentServiceSrv.Iface client = clientBuilder.build(PaymentServiceSrv.Iface.class);

        //Insert payment row
        dev.vality.damsel.fraudbusters.Payment payment = BeanUtil.createPayment(PaymentStatus.captured);
        payment.setId(ID_PAYMENT);
        payment.getClientInfo().setEmail(EMAIL);
        client.insertPayments(List.of(payment));
        await().atMost(30, SECONDS)
                .until(() -> jdbcTemplate.queryForList(SELECT_FROM_PAYMENT).size() == 1);

        //Insert fraud row
        client.insertFraudPayments(List.of(FraudPaymentRepositoryTest.createFraudPayment(ID_PAYMENT)));
        await().atMost(30, SECONDS)
                .until(() -> jdbcTemplate.queryForList(SELECT_FROM_FRAUD_FRAUD_PAYMENT).size() == 1);

        List<Map<String, Object>> maps = jdbcTemplate.queryForList(SELECT_FROM_FRAUD_FRAUD_PAYMENT);
        assertEquals(EMAIL, maps.get(0).get("email"));
    }

}
