package dev.vality.fraudbusters.fraud.payment;

import dev.vality.fraudbusters.config.payment.PaymentFraudoConfig;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.UniqueValueAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.PaymentModelFieldResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.PaymentTypeResolverImpl;
import dev.vality.fraudbusters.fraud.payment.validator.PaymentTemplateValidator;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.service.TimeBoundaryServiceImpl;
import dev.vality.fraudo.aggregator.UniqueValueAggregator;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.fraudo.finder.InListFinder;
import dev.vality.fraudo.model.ResultModel;
import dev.vality.fraudo.payment.aggregator.CountPaymentAggregator;
import dev.vality.fraudo.payment.aggregator.SumPaymentAggregator;
import dev.vality.fraudo.payment.resolver.CustomerTypeResolver;
import dev.vality.fraudo.payment.visitor.impl.FirstFindVisitorImpl;
import dev.vality.fraudo.resolver.CountryResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManyEmailsPerCardRuleTest {

    private static final String CARD_TOKEN = "card-token";
    private static final String EMAIL = "test_af_as@test.com";
    private static final long TIMESTAMP = Instant.parse("2026-08-11T07:43:52Z").toEpochMilli();
    private static final String UNPARENTHESIZED_RULE = """
            rule: many_emails_per_card:
                not in("card_token", "UNKNOWN")
                and not in("email", "UNKNOWN")
                and unique("card_token", "email", 24, hours) > 5
                -> decline;
            """;
    private static final String RULE = """
            rule: many_emails_per_card:
                (not in("card_token", "UNKNOWN"))
                and (not in("email", "UNKNOWN"))
                and unique("card_token", "email", 24, hours) > 5
                -> decline;
            """;

    @Test
    void unparenthesizedNotNegatesTheRemainingExpressionAndSkipsUnique() {
        RepositoryStub repository = new RepositoryStub(1);

        ResultModel result = applyRule(UNPARENTHESIZED_RULE, createPayment(CARD_TOKEN), repository);

        assertEquals(1, result.getRuleResults().size());
        assertEquals(ResultStatus.DECLINE, result.getRuleResults().get(0).getResultStatus());
        assertEquals("many_emails_per_card", result.getRuleResults().get(0).getRuleChecked());
        assertNull(repository.getAggregationFields());
    }

    @Test
    void doesNotDeclineWhenFunctionResultIsTwo() {
        RepositoryStub repository = new RepositoryStub(1);

        ResultModel result = applyRule(RULE, createPayment(CARD_TOKEN), repository);

        assertTrue(result.getRuleResults().isEmpty(), result.toString());
        assertArrayEquals(
                new Object[]{"cardToken", CARD_TOKEN, "email"},
                repository.getAggregationFields()
        );
    }

    @Test
    void declinesWhenFunctionResultIsSix() {
        RepositoryStub repository = new RepositoryStub(5);

        ResultModel result = applyRule(RULE, createPayment(CARD_TOKEN), repository);

        assertEquals(1, result.getRuleResults().size());
        assertEquals(ResultStatus.DECLINE, result.getRuleResults().get(0).getResultStatus());
        assertEquals("many_emails_per_card", result.getRuleResults().get(0).getRuleChecked());
        assertArrayEquals(
                new Object[]{"cardToken", CARD_TOKEN, "email"},
                repository.getAggregationFields()
        );
    }

    @Test
    void doesNotQueryHistoryForUnknownCardToken() {
        RepositoryStub repository = new RepositoryStub(100);

        ResultModel result = applyRule(RULE, createPayment(UNKNOWN), repository);

        assertTrue(result.getRuleResults().isEmpty());
        assertNull(repository.getAggregationFields());
    }

    @Test
    void doesNotQueryHistoryForNullCardToken() {
        RepositoryStub repository = new RepositoryStub(100);

        ResultModel result = applyRule(RULE, createPayment(null), repository);

        assertTrue(result.getRuleResults().isEmpty());
        assertNull(repository.getAggregationFields());
    }

    private ResultModel applyRule(String rule, PaymentModel paymentModel, RepositoryStub repository) {
        assertTrue(new PaymentTemplateValidator().validate(rule).isEmpty());
        FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> visitor = createVisitor(repository.asRepository());
        return visitor.visit(new PaymentContextParserImpl().parse(rule), paymentModel);
    }

    private FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> createVisitor(PaymentRepository repository) {
        DatabasePaymentFieldResolver databaseResolver = new DatabasePaymentFieldResolver();
        UniqueValueAggregator<PaymentModel, PaymentCheckedField> uniqueAggregator =
                new UniqueValueAggregatorImpl(databaseResolver, repository, new TimeBoundaryServiceImpl());
        PaymentFraudoConfig config = new PaymentFraudoConfig();
        return config.paymentRuleVisitor(
                unusedProxy(CountPaymentAggregator.class),
                unusedProxy(SumPaymentAggregator.class),
                uniqueAggregator,
                unusedProxy(CountryResolver.class),
                unusedProxy(InListFinder.class),
                new PaymentModelFieldResolver(),
                new PaymentTypeResolverImpl(),
                unusedProxy(CustomerTypeResolver.class)
        );
    }

    private PaymentModel createPayment(String cardToken) {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setCardToken(cardToken);
        paymentModel.setEmail(EMAIL);
        paymentModel.setTimestamp(TIMESTAMP);
        return paymentModel;
    }

    @SuppressWarnings("unchecked")
    private <T> T unusedProxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type == Boolean.class || type == boolean.class) {
            return false;
        }
        if (type == Integer.class || type == int.class) {
            return 0;
        }
        if (type == Long.class || type == long.class) {
            return 0L;
        }
        if (type == Double.class || type == double.class) {
            return 0D;
        }
        return null;
    }

    private static final class RepositoryStub {

        private final int historicalCount;
        private final AtomicReference<Object[]> aggregationArguments = new AtomicReference<>();

        private RepositoryStub(int historicalCount) {
            this.historicalCount = historicalCount;
        }

        private PaymentRepository asRepository() {
            return (PaymentRepository) Proxy.newProxyInstance(
                    PaymentRepository.class.getClassLoader(),
                    new Class<?>[]{PaymentRepository.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("uniqCountOperationWithGroupBy")) {
                            aggregationArguments.set(args);
                            return historicalCount;
                        }
                        return null;
                    }
            );
        }

        private Object[] getAggregationFields() {
            Object[] arguments = aggregationArguments.get();
            if (arguments == null) {
                return null;
            }
            return new Object[]{arguments[0], arguments[1], arguments[2]};
        }
    }
}
