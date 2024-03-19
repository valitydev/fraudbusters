package dev.vality.fraudbusters;

import dev.vality.damsel.domain.RiskScore;
import dev.vality.damsel.fraudbusters.Command;
import dev.vality.damsel.proxy_inspector.Context;
import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.repository.clickhouse.impl.FraudResultRepository;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.clickhouse.ClickHouseDataSource;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ActiveProfiles("full-prod")
@KafkaTestcontainer(
        properties = {
                "kafka.listen.result.concurrency=1"},
        topicsKeys = {
                "kafka.topic.template",
                "kafka.topic.reference"})
@ContextConfiguration(classes = {KafkaProducerConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT,
        properties = {"kafka.listen.result.concurrency=1", "kafka.historical.listener.enable=true"})
@Import({MockExternalServiceConfig.class})
class PreLoadTest {

    private static final String TEMPLATE = "rule: 12 >= 1\n" +
            " -> accept;";
    private static final String TEST = "test";

    @MockBean
    ClickHouseDataSource clickHouseDataSource;
    @MockBean
    FraudResultRepository paymentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
    @Autowired
    private KafkaTopics kafkaTopics;
    @Autowired
    private HistoricalPool<ParserRuleContext> timeTemplateTimePoolImpl;
    @Autowired
    @Qualifier("timeReferencePoolImpl")
    private HistoricalPool<String> timeReferencePoolImpl;

    @LocalServerPort
    int serverPort;

    private InspectorProxySrv.Iface client;

    @Test
    public void inspectPaymentTest() throws URISyntaxException, TException {
        Command crateCommandTemplate =
                TestObjectsFactory.createCommandTemplate(TEST, TEMPLATE);
        testThriftKafkaProducer.send(kafkaTopics.getFullTemplate(), crateCommandTemplate);
        Command crateCommandReference = TestObjectsFactory.crateCommandReference(TEST);
        testThriftKafkaProducer.send(kafkaTopics.getFullReference(), crateCommandReference);

        THClientBuilder clientBuilder = new THClientBuilder()
                .withAddress(new URI(String.format("http://localhost:%s/fraud_inspector/v1", serverPort)))
                .withNetworkTimeout(300000);
        client = clientBuilder.build(InspectorProxySrv.Iface.class);

        await().atMost(15, SECONDS).until(() -> timeTemplateTimePoolImpl.size() == 1);
        await().atMost(15, SECONDS).until(() -> timeReferencePoolImpl.size() == 1);

        Context context = BeanUtil.createContext();
        await().atMost(15, SECONDS).until(() ->
                RiskScore.low.equals(client.inspectPayment(context)));
    }

}
