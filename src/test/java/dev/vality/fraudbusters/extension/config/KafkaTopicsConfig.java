package dev.vality.fraudbusters.extension.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@TestConfiguration
public class KafkaTopicsConfig {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public KafkaAdmin adminClient() {
        return new KafkaAdmin(kafkaProperties.buildAdminProperties());
    }

    @Bean
    public NewTopic wbTopic() {
        return createTopic("wb-list-event-sink");
    }

    @Bean
    public NewTopic resultTopic() {
        return createTopic("result");
    }

    @Bean
    public NewTopic fraudPaymentTopic() {
        return createTopic("fraud_payment");
    }

    @Bean
    public NewTopic paymentEventTopic() {
        return createTopic("payment_event");
    }

    @Bean
    public NewTopic refundEventTopic() {
        return createTopic("refund_event");
    }

    @Bean
    public NewTopic chargebackEventTopic() {
        return createTopic("chargeback_event");
    }

    @Bean
    public NewTopic templateTopic() {
        return createTopic("template");
    }

    @Bean
    public NewTopic fullTemplateTopic() {
        return createTopic("full_template");
    }

    @Bean
    public NewTopic templateReferenceTopic() {
        return createTopic("template_reference");
    }

    @Bean
    public NewTopic fullTemplateReferenceTopic() {
        return createTopic("full_template_reference");
    }

    @Bean
    public NewTopic groupListTopic() {
        return createTopic("group_list");
    }

    @Bean
    public NewTopic fullGroupListTopic() {
        return createTopic("full_group_list");
    }

    @Bean
    public NewTopic groupReferenceTopic() {
        return createTopic("group_reference");
    }

    @Bean
    public NewTopic fullGroupReferenceListTopic() {
        return createTopic("full_group_reference");
    }

    private NewTopic createTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .build();
    }

}
