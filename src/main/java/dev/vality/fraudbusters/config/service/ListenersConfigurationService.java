package dev.vality.fraudbusters.config.service;

import dev.vality.kafka.common.exception.handler.SeekToCurrentWithSleepBatchErrorHandler;
import dev.vality.damsel.fraudbusters.Command;
import dev.vality.fraudbusters.serde.CommandDeserializer;
import dev.vality.fraudbusters.service.ConsumerGroupIdService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.LoggingErrorHandler;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListenersConfigurationService {

    public static final long THROTTLING_TIMEOUT = 500L;
    public static final int MAX_WAIT_FETCH_MS = 7000;

    private static final String EARLIEST = "earliest";

    private final ConsumerGroupIdService consumerGroupIdService;
    private final KafkaProperties kafkaProperties;

    @Value("${kafka.max.poll.records}")
    private String maxPollRecords;
    @Value("${kafka.max.retry.attempts}")
    private int maxRetryAttempts;
    @Value("${spring.kafka.properties.backoff.interval}")
    private int backoffInterval;
    @Value("${kafka.listen.result.concurrency}")
    private int listenResultConcurrency;
    @Value("${kafka.dgraph.topics.payment.concurrency}")
    private int dgraphPaymentConcurrency;

    public Map<String, Object> createDefaultProperties(String groupId) {
        final Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return props;
    }

    public ConcurrentKafkaListenerContainerFactory<String, Command> createDefaultFactory(
            ConsumerFactory<String, Command> stringCommandConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Command> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringCommandConsumerFactory);
        factory.setConcurrency(1);
        factory.setRetryTemplate(retryTemplate());
        factory.setErrorHandler(new LoggingErrorHandler());
        return factory;
    }

    /*
     * Retry template.
     */
    private RetryPolicy retryPolicy() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(maxRetryAttempts);
        return policy;
    }

    private BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(backoffInterval);
        return policy;
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy());
        template.setBackOffPolicy(backOffPolicy());
        return template;
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(
            Deserializer<T> deserializer,
            String groupId) {
        String consumerGroup = consumerGroupIdService.generateGroupId(groupId);
        final Map<String, Object> props = createDefaultProperties(consumerGroup);
        return createFactoryWithProps(deserializer, props);
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(
            Deserializer<T> deserializer,
            String groupId,
            Integer fetchMinBytes) {
        String consumerGroup = consumerGroupIdService.generateGroupId(groupId);
        final Map<String, Object> props = createDefaultProperties(consumerGroup);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, MAX_WAIT_FETCH_MS);
        return createFactoryWithProps(deserializer, props);
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> createDgraphFactory(
            Deserializer<T> deserializer,
            String groupId,
            Integer fetchMinBytes) {
        String consumerGroup = consumerGroupIdService.generateGroupId(groupId);
        final Map<String, Object> props = createDefaultProperties(consumerGroup);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, MAX_WAIT_FETCH_MS);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        final ConcurrentKafkaListenerContainerFactory<String, T> factory = createFactoryWithProps(deserializer, props);
        factory.setBatchErrorHandler(new SeekToCurrentWithSleepBatchErrorHandler());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(dgraphPaymentConcurrency);
        return factory;
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactoryWithProps(
            Deserializer<T> deserializer,
            Map<String, Object> props) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(listenResultConcurrency);
        factory.setBatchListener(true);
        return factory;
    }

    public ConsumerFactory<String, Command> createDefaultConsumerFactory(String groupListReferenceGroupId) {
        String value = consumerGroupIdService.generateRandomGroupId(groupListReferenceGroupId);
        final Map<String, Object> props = createDefaultProperties(value);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new CommandDeserializer());
    }

}
