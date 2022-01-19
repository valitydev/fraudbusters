package dev.vality.fraudbusters.config.payment;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.fraudbusters.config.service.KafkaTemplateConfigurationService;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.constant.GroupPostfix;
import dev.vality.fraudbusters.domain.FraudResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@RequiredArgsConstructor
public class PaymentKafkaConfig {

    private final ListenersConfigurationService listenersConfigurationService;
    private final KafkaTemplateConfigurationService kafkaTemplateConfigurationService;

    @Bean
    public ConsumerFactory<String, Command> templateListenerFactory() {
        return listenersConfigurationService.createDefaultConsumerFactory(GroupPostfix.TEMPLATE_GROUP_ID);
    }

    @Bean
    public ConsumerFactory<String, Command> groupReferenceListenerFactory() {
        return listenersConfigurationService.createDefaultConsumerFactory(GroupPostfix.GROUP_LIST_REFERENCE_GROUP_ID);
    }

    @Bean
    public ConsumerFactory<String, Command> referenceListenerFactory() {
        return listenersConfigurationService.createDefaultConsumerFactory(GroupPostfix.REFERENCE_GROUP_ID);
    }

    @Bean
    public ConsumerFactory<String, Command> groupListenerFactory() {
        return listenersConfigurationService.createDefaultConsumerFactory(GroupPostfix.GROUP_LIST_GROUP_ID);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Command> groupListenerContainerFactory(
            ConsumerFactory<String, Command> groupListenerFactory) {
        return listenersConfigurationService.createDefaultFactory(groupListenerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Command> templateListenerContainerFactory(
            ConsumerFactory<String, Command> templateListenerFactory) {
        return listenersConfigurationService.createDefaultFactory(templateListenerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Command> referenceListenerContainerFactory(
            ConsumerFactory<String, Command> referenceListenerFactory) {
        return listenersConfigurationService.createDefaultFactory(referenceListenerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Command> groupReferenceListenerContainerFactory(
            ConsumerFactory<String, Command> groupReferenceListenerFactory) {
        return listenersConfigurationService.createDefaultFactory(groupReferenceListenerFactory);
    }

    @Bean
    public KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate() {
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(kafkaTemplateConfigurationService.producerJsonConfigs())
        );
    }
}
