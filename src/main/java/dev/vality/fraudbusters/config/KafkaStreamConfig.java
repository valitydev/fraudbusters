package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.config.properties.KafkaSslProperties;
import dev.vality.fraudbusters.serde.CommandSerde;
import dev.vality.fraudbusters.service.ConsumerGroupIdService;
import dev.vality.fraudbusters.util.SslKafkaUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaStreamConfig {

    public static final String SENDER = "sender";
    private final ConsumerGroupIdService consumerGroupIdService;
    private final KafkaSslProperties kafkaSslProperties;
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    @Value("${kafka.reconnect-backoff-ms}")
    private int reconnectBackoffMs;
    @Value("${kafka.reconnect-backoff-max-ms}")
    private int reconnectBackoffMaxMs;
    @Value("${kafka.retry-backoff-ms}")
    private int retryBackoffMs;

    @Bean
    public Properties rewriteStreamProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, consumerGroupIdService.generateGroupId(SENDER));
        props.put(StreamsConfig.CLIENT_ID_CONFIG, consumerGroupIdService.generateGroupId(SENDER));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, CommandSerde.class);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
//        props.put(StreamsConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectBackoffMs);
//        props.put(StreamsConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectBackoffMaxMs);
//        props.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        props.put(
                StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndContinueExceptionHandler.class
        );
        props.putAll(SslKafkaUtils.sslConfigure(kafkaSslProperties));
        return props;
    }

}
