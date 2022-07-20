package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.serde.CommandSerde;
import dev.vality.fraudbusters.service.ConsumerGroupIdService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaStreamConfig {

    public static final String SENDER = "sender";
    private final ConsumerGroupIdService consumerGroupIdService;

    @Bean
    public Properties rewriteStreamProperties(KafkaProperties kafkaProperties) {
        final Map<String, Object> props = kafkaProperties.buildStreamsProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, consumerGroupIdService.generateGroupId(SENDER));
        props.put(StreamsConfig.CLIENT_ID_CONFIG, consumerGroupIdService.generateGroupId(SENDER));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, CommandSerde.class);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(
                StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndContinueExceptionHandler.class
        );

        var properties = new Properties();
        properties.putAll(props);
        return properties;
    }

}
