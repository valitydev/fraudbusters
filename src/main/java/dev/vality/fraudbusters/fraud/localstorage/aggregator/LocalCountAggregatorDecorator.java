package dev.vality.fraudbusters.fraud.localstorage.aggregator;

import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.CountPaymentAggregator;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.AggregateGroupingFunction;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.CountAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LocalCountAggregatorDecorator implements CountPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private final CountAggregatorImpl countAggregator;
    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final LocalResultStorageRepository localStorageRepository;

    @Override
    public Integer count(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        Integer count = countAggregator.count(checkedField, paymentModel, timeWindow, list);
        FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
        Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
        Instant instantFrom = Instant.ofEpochMilli(TimestampUtil.generateTimestampMinusMinutesMillis(
                now,
                timeWindow.getStartWindowTime()
        ));
        Instant instantTo = Instant.ofEpochMilli(TimestampUtil.generateTimestampMinusMinutesMillis(
                now,
                timeWindow.getEndWindowTime()
        ));
        Integer localCount = localStorageRepository.countOperationByField(checkedField.name(), resolve.getValue(),
                instantFrom.getEpochSecond(),
                instantTo.getEpochSecond()
        );
        return localCount + count;
    }

    @Override
    public Integer countSuccess(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        Integer countError = countAggregator.countSuccess(checkedField, paymentModel, timeWindow, list);
        Integer resultCount = getCount(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                localStorageRepository::countOperationSuccessWithGroupBy
        ) + countError;
        log.debug("LocalStorageCountAggregatorImpl resultCount: {}", resultCount);
        return resultCount;
    }

    @Override
    public Integer countError(
            PaymentCheckedField checkedField, PaymentModel paymentModel, TimeWindow timeWindow,
            String errorCode, List<PaymentCheckedField> list) {
        try {
            Integer countError = countAggregator.countError(checkedField, paymentModel, timeWindow, errorCode, list);
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Instant instantFrom = Instant.ofEpochMilli(TimestampUtil.generateTimestampMinusMinutesMillis(
                    now,
                    timeWindow.getStartWindowTime()
            ));
            Instant instantTo = Instant.ofEpochMilli(TimestampUtil.generateTimestampMinusMinutesMillis(
                    now,
                    timeWindow.getEndWindowTime()
            ));
            Integer localCount = localStorageRepository.countOperationErrorWithGroupBy(
                    checkedField.name(),
                    resolve.getValue(),
                    instantFrom.getEpochSecond(),
                    instantTo.getEpochSecond(),
                    eventFields,
                    errorCode
            );
            int result = localCount + countError;
            log.debug(
                    "LocalStorageCountAggregatorImpl field: {} value: {}  countError: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    result
            );
            return result;
        } catch (Exception e) {
            log.warn("LocalStorageCountAggregatorImpl error when countError e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    public Integer countChargeback(
            PaymentCheckedField checkedField,
            PaymentModel model,
            TimeWindow timeWindow,
            List<PaymentCheckedField> fields) {
        return countAggregator.countChargeback(checkedField, model, timeWindow, fields);
    }

    @Override
    public Integer countRefund(
            PaymentCheckedField checkedField,
            PaymentModel model,
            TimeWindow timeWindow,
            List<PaymentCheckedField> fields) {
        return countAggregator.countRefund(checkedField, model, timeWindow, fields);
    }

    @NotNull
    private Integer getCount(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Integer> aggregateFunction) {
        try {
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Integer count = aggregateFunction.accept(
                    resolve.getName(),
                    resolve.getValue(),
                    TimestampUtil.generateTimestampMinusMinutesMillis(now, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(now, timeWindow.getEndWindowTime()),
                    eventFields
            );
            log.debug(
                    "LocalStorageCountAggregatorImpl field: {} value: {}  count: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    count
            );
            return count;
        } catch (Exception e) {
            log.warn("LocalStorageCountAggregatorImpl error when getCount e: ", e);
            throw new RuleFunctionException(e);
        }
    }
}
