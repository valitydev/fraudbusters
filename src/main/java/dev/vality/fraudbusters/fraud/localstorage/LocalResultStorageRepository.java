package dev.vality.fraudbusters.fraud.localstorage;

import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.fraud.filter.PaymentFieldValueFilter;
import dev.vality.fraudbusters.fraud.filter.PaymentFieldValueResolver;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalResultStorageRepository implements PaymentRepository {

    private final LocalResultStorage localStorage;
    private final PaymentFieldValueFilter paymentFieldValueFilter;
    private final PaymentFieldValueResolver paymentFieldValueResolver;

    @Override
    public Integer countOperationByField(String fieldName, Object value, Long from, Long to) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        int count = (int) checkedPayments.stream()
                .filter(checkedPayment -> checkedPayment.getEventTime() >= from
                        && checkedPayment.getEventTime() <= to
                        && paymentFieldValueFilter.filter(fieldName, value, checkedPayment))
                .count();
        log.debug("LocalResultStorageRepository countOperationByField: {}", count);
        return count;
    }

    @Override
    public Integer countOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        int count = (int) checkedPayments.stream()
                .filter(checkedPayment -> filterPaymentByValue(from, to, fieldModels, checkedPayment))
                .count();
        log.debug("LocalResultStorageRepository countOperationByFieldWithGroupBy: {}", count);
        return count;
    }

    @Override
    public Long sumOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        long sum = checkedPayments.stream()
                .filter(checkedPayment -> filterPaymentByValue(from, to, fieldModels, checkedPayment))
                .mapToLong(CheckedPayment::getAmount)
                .sum();
        log.debug("LocalResultStorageRepository sumOperationByFieldWithGroupBy: {}", sum);
        return sum;
    }

    @Override
    public Integer uniqCountOperation(String fieldNameBy, Object value, String fieldNameCount, Long from, Long to) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        long count = checkedPayments.stream()
                .filter(checkedPayment -> checkedPayment.getEventTime() >= from
                        && checkedPayment.getEventTime() <= to
                        && paymentFieldValueFilter.filter(fieldNameBy, value, checkedPayment))
                .map(checkedPayment -> paymentFieldValueResolver.resolve(fieldNameCount, checkedPayment))
                .distinct()
                .count();
        log.debug("LocalResultStorageRepository uniqCountOperation: {}", count);
        return (int) count;
    }

    @Override
    public Integer uniqCountOperationWithGroupBy(
            String fieldNameBy,
            Object value,
            String fieldNameCount,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        long count = checkedPayments.stream()
                .filter(checkedPayment -> filterPaymentByValue(from, to, fieldModels, checkedPayment))
                .map(checkedPayment -> paymentFieldValueResolver.resolve(fieldNameCount, checkedPayment))
                .distinct()
                .count();
        log.debug("LocalResultStorageRepository uniqCountOperation: {}", count);
        return (int) count;
    }

    private boolean filterPaymentByValue(
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            CheckedPayment checkedPayment) {
        return checkedPayment.getEventTime() >= from
                && checkedPayment.getEventTime() <= to
                && fieldModels.stream()
                .allMatch(fieldModel -> paymentFieldValueFilter.filter(
                        fieldModel.getName(),
                        fieldModel.getValue(),
                        checkedPayment
                ));
    }

    @Override
    public Integer countOperationSuccessWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return (int) checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.captured
                ))
                .count();
    }

    @Override
    public Integer countOperationPendingWithGroupBy(String fieldName, Object value, Long from, Long to,
                                                    List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return (int) checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.pending
                ))
                .count();
    }

    @Override
    public Integer countOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            String errorCode) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return (int) checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.failed
                )
                        && errorCode.equals(checkedPayment.getErrorCode()))
                .count();
    }


    @Override
    public Integer countOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return (int) checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.failed
                )).count();
    }

    @Override
    public Long sumOperationSuccessWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.captured
                ))
                .mapToLong(CheckedPayment::getAmount)
                .sum();
    }

    @Override
    public Long sumOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            String errorCode) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.failed
                )
                        && errorCode.equals(checkedPayment.getErrorCode()))
                .mapToLong(CheckedPayment::getAmount)
                .sum();
    }

    @Override
    public Long sumOperationErrorWithGroupBy(String fieldName,
                                             Object value,
                                             Long from,
                                             Long to,
                                             List<FieldModel> fieldModels) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        return checkedPayments.stream()
                .filter(checkedPayment -> filterByStatusAndFields(
                        from,
                        to,
                        fieldModels,
                        checkedPayment,
                        PaymentStatus.failed
                ))
                .mapToLong(CheckedPayment::getAmount)
                .sum();
    }

    @Override
    public Boolean isExistByField(String fieldName, Object value, Long from, Long to) {
        List<CheckedPayment> checkedPayments = localStorage.get();
        int count = (int) checkedPayments.stream()
                .filter(checkedPayment -> checkedPayment.getEventTime() >= from
                                          && checkedPayment.getEventTime() <= to
                                          && paymentFieldValueFilter.filter(fieldName, value, checkedPayment))
                .count();
        log.debug("LocalResultStorageRepository countOperationByField: {}", count);
        return count != 0;
    }

    private boolean filterByStatusAndFields(
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            CheckedPayment checkedPayment,
            PaymentStatus paymentStatus) {
        return checkedPayment.getEventTime() >= from
                && checkedPayment.getEventTime() <= to
                && fieldModels.stream()
                .allMatch(fieldModel -> paymentFieldValueFilter.filter(
                        fieldModel.getName(),
                        fieldModel.getValue(),
                        checkedPayment
                ))
                && paymentStatus.name().equals(checkedPayment.getPaymentStatus());
    }

}
