package dev.vality.fraudbusters.fraud.localstorage;

import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.filter.PaymentFieldValueFilter;
import dev.vality.fraudbusters.fraud.filter.PaymentFieldValueResolver;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalResultStorageRepositoryTest {

    private static final long FROM = 50L;
    private static final long TO = 150L;
    private static final String TOKEN = "token-a";

    private LocalResultStorageRepository repository;

    @BeforeEach
    void setUp() {
        LocalResultStorage storage = new LocalResultStorage();
        PaymentFieldValueResolver resolver = new PaymentFieldValueResolver();
        repository = new LocalResultStorageRepository(
                storage,
                new PaymentFieldValueFilter(resolver),
                resolver
        );

        storage.get().addAll(List.of(
                payment(TOKEN, "same@example.com", PaymentStatus.captured, null, 10L),
                payment(TOKEN, "same@example.com", PaymentStatus.pending, null, 20L),
                payment(TOKEN, "other@example.com", PaymentStatus.failed, "E1", 30L),
                payment(TOKEN, "other@example.com", PaymentStatus.failed, "E2", 40L),
                payment("token-b", "one@example.com", PaymentStatus.captured, null, 100L),
                payment("token-c", "two@example.com", PaymentStatus.failed, "E1", 200L)
        ));
    }

    @Test
    void shouldApplyMainFieldFilterToGeneralAggregations() {
        assertEquals(4, repository.countOperationByFieldWithGroupBy(
                PaymentCheckedField.CARD_TOKEN.name(), TOKEN, FROM, TO, List.of()));
        assertEquals(100L, repository.sumOperationByFieldWithGroupBy(
                PaymentCheckedField.CARD_TOKEN.name(), TOKEN, FROM, TO, List.of()));
        assertEquals(2, repository.uniqCountOperationWithGroupBy(
                PaymentCheckedField.CARD_TOKEN.name(),
                TOKEN,
                PaymentCheckedField.EMAIL.name(),
                FROM,
                TO,
                List.of()
        ));
    }

    @Test
    void shouldApplyMainFieldFilterToStatusCounts() {
        String fieldName = PaymentCheckedField.CARD_TOKEN.name();

        assertEquals(1, repository.countOperationSuccessWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of()));
        assertEquals(1, repository.countOperationPendingWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of()));
        assertEquals(2, repository.countOperationErrorWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of()));
        assertEquals(1, repository.countOperationErrorWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of(), "E1"));
    }

    @Test
    void shouldApplyMainFieldFilterToStatusSums() {
        String fieldName = PaymentCheckedField.CARD_TOKEN.name();

        assertEquals(10L, repository.sumOperationSuccessWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of()));
        assertEquals(70L, repository.sumOperationErrorWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of()));
        assertEquals(30L, repository.sumOperationErrorWithGroupBy(
                fieldName, TOKEN, FROM, TO, List.of(), "E1"));
    }

    @Test
    void shouldResolveDifferentFieldNamesForClickHouseAndLocalStorage() {
        DatabasePaymentFieldResolver resolver = new DatabasePaymentFieldResolver();
        PaymentModel payment = new PaymentModel();
        payment.setCardToken(TOKEN);

        List<FieldModel> clickHouseFields =
                resolver.resolveListFields(payment, List.of(PaymentCheckedField.CARD_TOKEN));
        List<FieldModel> localFields =
                resolver.resolveListFieldsForLocalStorage(payment, List.of(PaymentCheckedField.CARD_TOKEN));

        assertEquals("cardToken", clickHouseFields.get(0).getName());
        assertEquals("CARD_TOKEN", localFields.get(0).getName());
    }

    private CheckedPayment payment(
            String cardToken,
            String email,
            PaymentStatus status,
            String errorCode,
            Long amount) {
        CheckedPayment payment = new CheckedPayment();
        payment.setEventTime(100L);
        payment.setCardToken(cardToken);
        payment.setEmail(email);
        payment.setPaymentStatus(status.name());
        payment.setErrorCode(errorCode);
        payment.setAmount(amount);
        return payment;
    }
}
