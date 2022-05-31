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
import dev.vality.fraudbusters.exception.DgraphException;
import dev.vality.fraudbusters.listener.events.dgraph.*;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.stream.impl.FullTemplateVisitorImpl;
import dev.vality.kafka.common.retry.ConfigurableRetryPolicy;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.dgraph.TxnConflictException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "dgraph.service.enabled", havingValue = "true")
public class DgraphConfig {

    private static final String DEFAULT_DGRAPH_ERROR_PREFIX = "Register dgraph transaction failed event. ";
    private static final String PKCS_12 = "PKCS12";

    @Bean
    public ObjectMapper dgraphObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public RetryTemplate dgraphRetryTemplate(DgraphProperties dgraphProperties) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(
                new ConfigurableRetryPolicy(
                        dgraphProperties.getMaxAttempts(),
                        Collections.singletonMap(RuntimeException.class, true)
                )
        );
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(dgraphProperties.getBackoffPeriod());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.registerListener(new RegisterJobFailListener());

        return retryTemplate;
    }

    @Bean
    public DgraphClient dgraphClient(DgraphProperties dgraphProperties, RetryTemplate dgraphRetryTemplate) {
        try {
            return dgraphRetryTemplate.execute(context -> createDgraphClient(dgraphProperties));
        } catch (Exception ex) {
            log.error("Received an exception while the service was creating the dgraph client instance", ex);
            throw new DgraphException(ex);
        }
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

    private DgraphClient createDgraphClient(DgraphProperties dgraphProperties) throws SSLException {
        log.info("Create dgraph client (targets: {})", dgraphProperties.getTargets());
        DgraphClient dgraphClient = new DgraphClient(createStubs(dgraphProperties.getTargets(), dgraphProperties));

        log.info("Dgraph version: {}", dgraphClient.checkVersion());
        dgraphClient.alter(
                DgraphProto.Operation.newBuilder()
                        .setSchema(DgraphSchemaConstants.SCHEMA)
                        .build()
        );
        log.info("Altering of the schema was completed");
        return dgraphClient;
    }

    private DgraphGrpc.DgraphStub[] createStubs(List<String> dgraphTargets,
                                                DgraphProperties dgraphProperties) throws SSLException {
        List<DgraphGrpc.DgraphStub> stubs = new ArrayList<>();
        for (String target : dgraphTargets) {
            stubs.add(createStub(target, dgraphProperties));
        }
        return stubs.toArray(new DgraphGrpc.DgraphStub[dgraphTargets.size()]);
    }

    private DgraphGrpc.DgraphStub createStub(String target, DgraphProperties dgraphProperties) throws SSLException {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget(target);
        channelBuilder.usePlaintext();
        if (dgraphProperties.isAuth()) {
            log.info("Connect to the Dgraph cluster with TLS config...");
            SslContext sslContext = GrpcSslContexts.forClient()
                    .trustManager(new File(dgraphProperties.getTrustCertCollectionFile()))
                    .keyManager(
                            new File(dgraphProperties.getKeyCertChainFile()),
                            new File(dgraphProperties.getKeyFile(), dgraphProperties.getKeyPassword())
                    )
                    .keyStoreType(PKCS_12)
                    .build();
            channelBuilder.sslContext(sslContext);
        }

        return DgraphGrpc.newStub(channelBuilder.build());
    }

    private static final class RegisterJobFailListener extends RetryListenerSupport {

        @Override
        public <T, E extends Throwable> void onError(RetryContext context,
                                                     RetryCallback<T, E> callback,
                                                     Throwable throwable) {
            if (throwable instanceof TxnConflictException
                    || context.getLastThrowable() instanceof TxnConflictException
                    || context.getLastThrowable().getCause() instanceof TxnConflictException) {
                log.info(DEFAULT_DGRAPH_ERROR_PREFIX + "Retry count: {}", context.getRetryCount());
                log.debug(DEFAULT_DGRAPH_ERROR_PREFIX + "Stacktrace", context.getLastThrowable());
            } else {
                log.warn(DEFAULT_DGRAPH_ERROR_PREFIX + "Unexpected error (exeption type: {})",
                        throwable.getClass().getName(), context.getLastThrowable());
            }
        }
    }

}
