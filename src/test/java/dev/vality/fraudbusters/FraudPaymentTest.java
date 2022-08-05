package dev.vality.fraudbusters;

import dev.vality.damsel.fraudbusters.PaymentServiceSrv;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.constants.TestProperties;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.FraudPaymentRepositoryTest;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
        insertWithTimeout(client, List.of(payment));

        //Insert fraud row
        client.insertFraudPayments(List.of(FraudPaymentRepositoryTest.createFraudPayment(ID_PAYMENT)));
        Thread.sleep(TestProperties.TIMEOUT * 10);

        //Check join and view working
        List<Map<String, Object>> maps = jdbcTemplate.queryForList("SELECT * from fraud.fraud_payment");
        assertEquals(1, maps.size());
        assertEquals(EMAIL, maps.get(0).get("email"));
    }

    private void insertWithTimeout(
            PaymentServiceSrv.Iface client,
            List<dev.vality.damsel.fraudbusters.Payment> payments) throws TException, InterruptedException {
        client.insertPayments(payments);
        Thread.sleep(TestProperties.TIMEOUT * 10);
    }

}
