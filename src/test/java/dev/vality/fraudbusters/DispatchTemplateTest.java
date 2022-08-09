package dev.vality.fraudbusters;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.fraudbusters.config.MockExternalServiceConfig;
import dev.vality.fraudbusters.config.properties.KafkaTopics;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.testcontainers.annotations.KafkaSpringBootTest;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("full-prod")
@ExtendWith({SpringExtension.class})
@KafkaTestcontainer(
        properties = {
                "kafka.listen.result.concurrency=1"},
        topicsKeys = {
                "kafka.topic.template",
                "kafka.topic.reference"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@KafkaSpringBootTest
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(MockExternalServiceConfig.class)
class DispatchTemplateTest {

    public static final String TEMPLATE = "rule: 12 >= 1 -> accept;";
    public static final int TIMEOUT = 5000;

    @Autowired
    private KafkaTopics kafkaTopics;

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @MockBean
    private Pool<ParserRuleContext> templatePoolImpl;
    @MockBean
    @Qualifier("referencePoolImpl")
    private Pool<String> referencePoolImpl;

    @Test
    void testTemplatePool() {
        String id = UUID.randomUUID().toString();
        Command command = TestObjectsFactory.createCommandTemplate(id, TEMPLATE);

        testThriftKafkaProducer.send(kafkaTopics.getTemplate(), command);

        verify(templatePoolImpl, timeout(TIMEOUT).times(1)).add(anyString(), any(ParserRuleContext.class));
    }

    @Test
    void testGlobalReferencePool() {
        String id = UUID.randomUUID().toString();
        Command command = TestObjectsFactory.crateCommandReference(id);

        testThriftKafkaProducer.send(kafkaTopics.getReference(), command);

        verify(referencePoolImpl, timeout(TIMEOUT).times(1)).add(anyString(), anyString());
    }
}
