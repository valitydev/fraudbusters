package dev.vality.fraudbusters.config.service;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.damsel.fraudbusters.ReferenceInfo;
import dev.vality.fraudbusters.config.properties.KafkaSslProperties;
import dev.vality.fraudbusters.util.SslKafkaUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaTemplateConfigurationService {

    private final KafkaSslProperties kafkaSslProperties;

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    public Map<String, Object> producerJsonConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.putAll(SslKafkaUtils.sslConfigure(kafkaSslProperties));
        return props;
    }

    public Map<String, Object> producerThriftConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        props.putAll(SslKafkaUtils.sslConfigure(kafkaSslProperties));
        return props;
    }

    @Bean
    public KafkaTemplate<String, ReferenceInfo> kafkaUnknownInitiatingEntityTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerThriftConfigs()));
    }
}
