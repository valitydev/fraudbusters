package dev.vality.fraudbusters.config.service;

import dev.vality.damsel.fraudbusters.ReferenceInfo;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaTemplateConfigurationService {

    private final  KafkaProperties kafkaProperties;

    public Map<String, Object> producerJsonConfigs() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    public Map<String, Object> producerThriftConfigs() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        return props;
    }

    @Bean
    public KafkaTemplate<String, ReferenceInfo> kafkaUnknownInitiatingEntityTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerThriftConfigs()));
    }
}
