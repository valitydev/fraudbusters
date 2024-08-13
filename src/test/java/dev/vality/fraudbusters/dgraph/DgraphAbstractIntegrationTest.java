package dev.vality.fraudbusters.dgraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fraudbusters.FraudBustersApplication;
import dev.vality.fraudbusters.dgraph.insert.model.Aggregates;
import dev.vality.fraudbusters.dgraph.insert.model.TestQuery;
import dev.vality.fraudbusters.exception.DgraphException;
import dev.vality.fraudbusters.extension.KafkaContainerExtension;
import dev.vality.fraudbusters.extension.config.KafkaTopicsConfig;
import dev.vality.fraudbusters.util.KeyGenerator;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.thrift.TBase;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@Import(KafkaTopicsConfig.class)
@ExtendWith({
        SpringExtension.class,
        KafkaContainerExtension.class
})
@SpringBootTest(webEnvironment = RANDOM_PORT,
        classes = FraudBustersApplication.class,
        properties = {
                "kafka.listen.result.concurrency=1",
                "dgraph.service.enabled=true",
                "spring.kafka.properties.reconnect.backoff.ms=100",
                "spring.kafka.properties.reconnect.backoff.max.ms=100",
                "spring.kafka.properties.retry.backoff.ms=100",
                "kafka.dgraph.topics.payment.enabled=true",
                "kafka.dgraph.topics.refund.enabled=true",
                "kafka.dgraph.topics.fraud_payment.enabled=true",
                "kafka.dgraph.topics.chargeback.enabled=true",
                "kafka.dgraph.topics.withdrawal.enabled=true",
                "dgraph.port=9080",
                "dgraph.withAuthHeader=false",
                "dgraph.negotiationType=PLAINTEXT"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Ignore
public abstract class DgraphAbstractIntegrationTest {

    @Autowired
    private ObjectMapper dgraphObjectMapper;

    @Autowired
    protected DgraphClient dgraphClient;

    private static GenericContainer dgraphServer;
    private static volatile boolean isDgraphStarted;
    private static String testHostname = "localhost";
    private static final int RETRIES_COUNT = 10;
    private static final long TIMEOUT = 5000L;

    @DynamicPropertySource
    static void connectionConfigs(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KafkaContainerExtension.KAFKA::getBootstrapServers);
        registry.add("dgraph.targets.[0]", () -> testHostname + ":9080");
    }

    @BeforeAll
    public static void setup() throws Exception {
        if (!isDgraphStarted) {
            startDgraphServer();
            cleanupBeforeTermination();
            isDgraphStarted = true;
        }

        int count = 0;
        while (!clearDb() && count < RETRIES_COUNT) {
            count++;
            Thread.sleep(TIMEOUT);
        }
    }

    private static boolean clearDb() {
        try {
            DgraphClient dgraphClient = new DgraphClient(createStub(testHostname, 9080));
            dgraphClient.alter(
                    DgraphProto.Operation.newBuilder()
                            .setDropAll(true)
                            .build()
            );
            return true;
        } catch (RuntimeException ex) {
            log.error("Received error while the service cleaned the database", ex);
            return false;
        }
    }

    private static DgraphGrpc.DgraphStub createStub(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        return DgraphGrpc.newStub(channel);
    }

    private static void cleanupBeforeTermination() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isDgraphStarted) {
                if (dgraphServer.isRunning()) {
                    dgraphServer.stop();
                }
            }
        }));
    }

    private static void startDgraphServer() throws InterruptedException {
        log.info("Creating dgraph server");
        dgraphServer = new GenericContainer<>("dgraph/standalone:latest")
                .withExposedPorts(8000, 8080, 9080)
                .withPrivilegedMode(true)
                .waitingFor(new WaitAllStrategy()
                        .withStartupTimeout(Duration.ofMinutes(2)));
        dgraphServer.setPortBindings(Arrays.asList("8000:8000", "8080:8080", "9080:9080"));
        dgraphServer.start();
        testHostname = dgraphServer.getHost();
        log.info("Dgraph server was created (host:{}, ip: {}, containerIp: {}, ports: {})", testHostname,
                dgraphServer.getIpAddress(), dgraphServer.getContainerIpAddress(), dgraphServer.getExposedPorts());
        Thread.sleep(5000);
    }

    protected Aggregates getAggregates(String query) {
        String responseJson = processQuery(query);
        log.debug("Received json with aggregates (query: {}): {}", query, responseJson);
        TestQuery testQuery = convertToObject(responseJson, TestQuery.class);
        return testQuery == null || testQuery.getAggregates() == null || testQuery.getAggregates().isEmpty()
                ? new Aggregates() : testQuery.getAggregates().get(0);
    }

    protected String processQuery(String query) {
        DgraphProto.Response response = processDgraphQuery(query);
        return response.getJson().toStringUtf8();
    }

    protected <T> T convertToObject(String json, Class<T> clazz) {
        try {
            return dgraphObjectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Received an exception when method was converting json to object", ex);
        }
    }

    protected DgraphProto.Response processDgraphQuery(String query) {
        try {
            return dgraphClient.newTransaction().query(query);
        } catch (RuntimeException ex) {
            throw new DgraphException(String.format("Received exception from dgraph while the service " +
                    "process ro query with args (query: %s)", query), ex);
        }
    }

    public int getCountOfObjects(String objectName) {
        String query = String.format("""
                query all() {
                    aggregates(func: type(%s)) {
                        count(uid)
                    }
                }
                """, objectName);

        return getAggregates(query).getCount();
    }

    public <T extends TBase> Producer<String, T> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerExtension.KAFKA.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, KeyGenerator.generateKey("client_id_"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, new ThriftSerializer<T>().getClass());
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        return new KafkaProducer<>(props);
    }

    <T> Consumer<String, T> createConsumer(Class valueDeserializerClazz) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerExtension.KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClazz);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    protected void waitingTopic(String topicName, Class valueDeserializerClazz) {
        try (Consumer<String, Object> consumer = createConsumer(valueDeserializerClazz)) {
            consumer.subscribe(List.of(topicName));
            Unreliables.retryUntilTrue(240, TimeUnit.SECONDS, () -> {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1L));
                return !records.isEmpty();
            });
        }
    }

    protected void checkCountOfObjects(String type, int count) {
        try {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .pollDelay(Durations.ONE_SECOND)
                    .until(() -> getCountOfObjects(type) == count);
        } catch (org.awaitility.core.ConditionTimeoutException ex) {
            String errorMessage = String.format("Received count of objects for type %s: %s (expected: %s)",
                    type, getCountOfObjects(type), count);
            throw new org.awaitility.core.ConditionTimeoutException(errorMessage, ex);
        }
    }

}
