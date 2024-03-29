package dev.vality.fraudbusters.fraud.localstorage.aggregator;

import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.AggregateGroupingFunction;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.SumAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.SumPaymentAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LocalSumAggregatorDecorator implements SumPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private final SumAggregatorImpl sumAggregatorImpl;
    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final LocalResultStorageRepository localStorageRepository;
    private final TimeBoundaryService timeBoundaryService;

    @Override
    public Double sum(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        Double sum = sumAggregatorImpl.sum(checkedField, paymentModel, timeWindow, list);
        FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
        Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
        TimeBound timeBound = timeBoundaryService.getBoundary(now, timeWindow);
        List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
        Long localSum = localStorageRepository.sumOperationByFieldWithGroupBy(
                checkedField.name(),
                resolve.getValue(),
                timeBound.getLeft().getEpochSecond(),
                timeBound.getRight().getEpochSecond(),
                eventFields
        );
        Double resultSum = checkedLong(localSum) + sum;
        log.debug("LocalSumAggregatorDecorator sum: {}", resultSum);
        return resultSum;
    }

    @Override
    public Double sumSuccess(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        Double sumSuccess = sumAggregatorImpl.sumSuccess(checkedField, paymentModel, timeWindow, list);
        Double resultCount = getSum(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                localStorageRepository::sumOperationSuccessWithGroupBy
        ) + sumSuccess;
        log.debug("LocalSumAggregatorDecorator sumSuccess: {}", resultCount);
        return resultCount;
    }

    @Override
    public Double sumError(PaymentCheckedField checkedField,
                           PaymentModel paymentModel,
                           TimeWindow timeWindow,
                           String errorCode,
                           List<PaymentCheckedField> list) {
        try {
            Double sumError = sumAggregatorImpl.sumError(checkedField, paymentModel, timeWindow, errorCode, list);
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            TimeBound timeBound = timeBoundaryService.getBoundary(now, timeWindow);
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Long localSum = localStorageRepository.sumOperationErrorWithGroupBy(
                    checkedField.name(),
                    resolve.getValue(),
                    timeBound.getLeft().getEpochSecond(),
                    timeBound.getRight().getEpochSecond(),
                    eventFields,
                    errorCode
            );
            Double result = checkedLong(localSum) + sumError;
            log.debug(
                    "LocalSumAggregatorDecorator field: {} value: {}  sumError: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    result
            );
            return result;
        } catch (Exception e) {
            log.warn("LocalSumAggregatorDecorator error when sumError e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    public Double sumError(PaymentCheckedField checkedField,
                           PaymentModel paymentModel,
                           TimeWindow timeWindow,
                           List<PaymentCheckedField> list) {
        try {
            Double sumError = sumAggregatorImpl.sumError(checkedField, paymentModel, timeWindow, list);
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            TimeBound timeBound = timeBoundaryService.getBoundary(now, timeWindow);
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Long localSum = localStorageRepository.sumOperationErrorWithGroupBy(
                    checkedField.name(),
                    resolve.getValue(),
                    timeBound.getLeft().getEpochSecond(),
                    timeBound.getRight().getEpochSecond(),
                    eventFields
            );
            Double result = checkedLong(localSum) + sumError;
            log.debug(
                    "LocalSumAggregatorDecorator field: {} value: {}  sumError: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    result
            );
            return result;
        } catch (Exception e) {
            log.warn("LocalSumAggregatorDecorator error when sumError e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    public Double sumChargeback(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return sumAggregatorImpl.sumChargeback(checkedField, paymentModel, timeWindow, list);
    }

    @Override
    public Double sumRefund(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return sumAggregatorImpl.sumRefund(checkedField, paymentModel, timeWindow, list);
    }

    @NotNull
    private Double getSum(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Long> aggregateFunction) {
        return getSum(checkedField, paymentModel, timeWindow, list, aggregateFunction, true);
    }

    @NotNull
    private Double getSum(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Long> aggregateFunction,
            boolean withCurrent) {
        try {
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            TimeBound timeBound = timeBoundaryService.getBoundary(now, timeWindow);
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Long sum = aggregateFunction.accept(
                    resolve.getName(),
                    resolve.getValue(),
                    timeBound.getLeft().toEpochMilli(),
                    timeBound.getRight().toEpochMilli(),
                    eventFields
            );
            double resultSum = withCurrent
                    ? (double) checkedLong(sum) + checkedLong(paymentModel.getAmount())
                    : checkedLong(sum);
            log.debug(
                    "LocalSumAggregatorDecorator field: {} value: {}  sum: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    resultSum
            );
            return resultSum;
        } catch (Exception e) {
            log.warn("LocalSumAggregatorDecorator error when getSum e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    private Long checkedLong(Long entry) {
        return entry != null ? entry : 0L;
    }
}
