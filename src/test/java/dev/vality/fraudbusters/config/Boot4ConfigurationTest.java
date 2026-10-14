package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.config.aggragations.AggregatorKafkaConfig;
import dev.vality.fraudbusters.config.service.KafkaTemplateConfigurationService;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.service.ConsumerGroupIdService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class Boot4ConfigurationTest {

    @Test
    void createsKafkaFactoriesAndJsonMapperWithoutConnectingToBroker() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withUserConfiguration(AggregatorKafkaConfig.class, KafkaStreamConfig.class,
                        KafkaTemplateConfigurationService.class, ListenersConfigurationService.class,
                        ConsumerGroupIdService.class)
                .withPropertyValues("kafka.consumer.prefix=fraudbusters", "kafka.max.poll.records=100",
                        "kafka.listen.result.concurrency=1", "kafka.aggr.payment.min.bytes=50000")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(JsonMapper.class);
                    assertThat(context).hasBean("kafkaPaymentResultListenerContainerFactory")
                            .hasBean("kafkaWithdrawalResultListenerContainerFactory")
                            .hasBean("kafkaRefundResultListenerContainerFactory")
                            .hasBean("kafkaChargebackResultListenerContainerFactory")
                            .hasBean("kafkaFraudPaymentListenerContainerFactory");
                    var listeners = context.getBean(ListenersConfigurationService.class);
                    assertThat(listeners.createDefaultProperties("test"))
                            .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                    var producers = context.getBean(KafkaTemplateConfigurationService.class);
                    assertThat(producers.producerJsonConfigs())
                            .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
                });
    }
}
