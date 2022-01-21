package dev.vality.fraudbusters.config.payment;

import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.fraudbusters.converter.CheckedResultToRiskScoreConverter;
import dev.vality.fraudbusters.converter.ContextToFraudRequestConverter;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.resource.payment.handler.FraudInspectorHandler;
import dev.vality.fraudbusters.stream.impl.TemplateVisitorImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class PaymentResourceConfig {

    @Value("${kafka.topic.result}")
    private String resultTopic;

    @Bean
    public InspectorProxySrv.Iface fraudInspectorHandler(
            KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate,
            CheckedResultToRiskScoreConverter checkedResultToRiskScoreConverter,
            ContextToFraudRequestConverter requestConverter,
            TemplateVisitorImpl templateVisitor) {
        return new FraudInspectorHandler(
                resultTopic,
                checkedResultToRiskScoreConverter,
                requestConverter,
                templateVisitor,
                kafkaFraudResultTemplate
        );
    }

}
