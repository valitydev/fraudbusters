package dev.vality.fraudbusters.config.dgraph;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.damsel.fraudbusters.Chargeback;
import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.damsel.fraudbusters.Withdrawal;
import dev.vality.fraudbusters.config.properties.DgraphProperties;
import dev.vality.fraudbusters.constant.DgraphSchemaConstants;
import dev.vality.fraudbusters.converter.PaymentToDgraphPaymentConverter;
import dev.vality.fraudbusters.converter.PaymentToPaymentModelConverter;
import dev.vality.fraudbusters.domain.dgraph.common.*;
import dev.vality.fraudbusters.listener.events.dgraph.*;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.stream.impl.FullTemplateVisitorImpl;
import dev.vality.kafka.common.retry.ConfigurableRetryPolicy;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "dgraph.service.enabled", havingValue = "true")
public class DgraphConfig {

    @Value("${dgraph.maxAttempts}")
    private int maxAttempts;

    @Bean
    public ObjectMapper dgraphObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public RetryTemplate dgraphRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(
                new ConfigurableRetryPolicy(maxAttempts, Collections.singletonMap(RuntimeException.class, true))
        );
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(10L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.registerListener(new RegisterJobFailListener());

        return retryTemplate;
    }

    @Bean
    public DgraphClient dgraphClient(DgraphProperties dgraphProperties) {
        log.info("Connecting to the dgraph cluster");
        String host = dgraphProperties.getHost();
        int port = dgraphProperties.getPort();
        log.info("Create dgraph client (host: {}, port: {})", host, port);
        DgraphClient dgraphClient = new DgraphClient(createStub(host, port));
        if (dgraphProperties.isAuth()) {
            log.info("Connect to the Dgraph cluster with login and password...");
            dgraphClient.login(dgraphProperties.getLogin(), dgraphProperties.getPassword());
        }
        log.info("Dgraph client was created (host: {}, port: {})", host, port);
        log.warn("Schema will be altered");
        dgraphClient.alter(
                DgraphProto.Operation.newBuilder()
                        .setSchema(DgraphSchemaConstants.SCHEMA)
                        .build()
        );
        return dgraphClient;
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.dgraph.topics.payment.enabled", havingValue = "true")
    public DgraphPaymentEventListener dgraphPaymentEventListener(
            Repository<DgraphPayment> dgraphPaymentRepository,
            FullTemplateVisitorImpl fullTemplateVisitor,
            PaymentToPaymentModelConverter paymentToPaymentModelConverter,
            PaymentToDgraphPaymentConverter paymentToDgraphPaymentConverter,
            ObjectMapper objectMapper
    ) {
        return new DgraphPaymentEventListener(
                dgraphPaymentRepository,
                fullTemplateVisitor,
                paymentToPaymentModelConverter,
                paymentToDgraphPaymentConverter,
                objectMapper
        );
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.dgraph.topics.fraud_payment.enabled", havingValue = "true")
    public DgraphFraudPaymentListener dgraphFraudPaymentListener(
            Repository<DgraphFraudPayment> repository,
            Converter<FraudPayment, DgraphFraudPayment> fraudPaymentToDgraphFraudPaymentConverter
    ) {
        return new DgraphFraudPaymentListener(repository, fraudPaymentToDgraphFraudPaymentConverter);
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.dgraph.topics.chargeback.enabled", havingValue = "true")
    public DgraphChargebackEventListener dgraphChargebackEventListener(
            Repository<DgraphChargeback> repository,
            Converter<Chargeback, DgraphChargeback> converter
    ) {
        return new DgraphChargebackEventListener(repository, converter);
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.dgraph.topics.refund.enabled", havingValue = "true")
    public DgraphRefundEventListener dgraphRefundEventListener(
            Repository<DgraphRefund> repository,
            Converter<Refund, DgraphRefund> converter
    ) {
        return new DgraphRefundEventListener(repository, converter);
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.dgraph.topics.withdrawal.enabled", havingValue = "true")
    public DgraphWithdrawalEventListener dgraphWithdrawalEventListener(
            Repository<DgraphWithdrawal> repository,
            Converter<Withdrawal, DgraphWithdrawal> converter
    ) {
        return new DgraphWithdrawalEventListener(repository, converter);
    }

    private DgraphGrpc.DgraphStub createStub(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        return DgraphGrpc.newStub(channel);
    }

    private static final class RegisterJobFailListener extends RetryListenerSupport {

        @Override
        public <T, E extends Throwable> void onError(RetryContext context,
                                                     RetryCallback<T, E> callback,
                                                     Throwable throwable) {
            log.warn("Register dgraph transaction failed event. Retry count: {}",
                    context.getRetryCount(), context.getLastThrowable());
        }
    }

}
