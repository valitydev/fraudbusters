package dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse;

import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.SumPaymentAggregator;
import dev.vality.fraudbusters.aspect.BasicMetric;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.AggregateGroupingFunction;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.AggregationRepository;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SumAggregatorImpl implements SumPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final PaymentRepository paymentRepository;
    private final AggregationRepository refundRepository;
    private final AggregationRepository chargebackRepository;

    @Override
    @BasicMetric("sum")
    public Double sum(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getSum(checkedField, paymentModel, timeWindow, list, paymentRepository::sumOperationByFieldWithGroupBy);
    }

    @Override
    @BasicMetric("sumSuccess")
    public Double sumSuccess(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getSum(checkedField, paymentModel, timeWindow, list, paymentRepository::sumOperationSuccessWithGroupBy);
    }

    @Override
    @BasicMetric("sumError")
    public Double sumError(
            PaymentCheckedField checkedField, PaymentModel paymentModel, TimeWindow timeWindow, String errorCode,
            List<PaymentCheckedField> list) {
        try {
            Instant timestamp = paymentModel.getTimestamp() != null
                    ? Instant.ofEpochMilli(paymentModel.getTimestamp())
                    : Instant.now();
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            if (StringUtils.isEmpty(resolve.getValue())) {
                return Double.valueOf(checkedLong(paymentModel.getAmount()));
            }
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Long sum = paymentRepository.sumOperationErrorWithGroupBy(
                    resolve.getName(),
                    resolve.getValue(),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getEndWindowTime()),
                    eventFields,
                    errorCode
            );
            double resultSum = (double) checkedLong(sum) + checkedLong(paymentModel.getAmount());
            log.debug(
                    "SumAggregatorImpl field: {} value: {}  sumError: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    resultSum
            );
            return resultSum;
        } catch (Exception e) {
            log.warn("SumAggregatorImpl error when sumError e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    public Double sumChargeback(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getSum(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                chargebackRepository::sumOperationByFieldWithGroupBy,
                false
        );
    }

    @Override
    public Double sumRefund(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getSum(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                refundRepository::sumOperationByFieldWithGroupBy,
                false
        );
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
    @BasicMetric("getSumWindowed")
    private Double getSum(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Long> aggregateFunction,
            boolean withCurrent) {
        try {
            Instant timestamp = paymentModel.getTimestamp() != null
                    ? Instant.ofEpochMilli(paymentModel.getTimestamp())
                    : Instant.now();
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            if (StringUtils.isEmpty(resolve.getValue())) {
                return Double.valueOf(checkedLong(paymentModel.getAmount()));
            }
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Long sum = aggregateFunction.accept(
                    resolve.getName(),
                    resolve.getValue(),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getEndWindowTime()),
                    eventFields
            );
            double resultSum =
                    withCurrent ? (double) checkedLong(sum) + checkedLong(paymentModel.getAmount()) : checkedLong(sum);
            log.debug(
                    "SumAggregatorImpl field: {} value: {}  sum: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    resultSum
            );
            return resultSum;
        } catch (Exception e) {
            log.warn("SumAggregatorImpl error when getSum e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    private Long checkedLong(Long entry) {
        return entry != null ? entry : 0L;
    }
}
