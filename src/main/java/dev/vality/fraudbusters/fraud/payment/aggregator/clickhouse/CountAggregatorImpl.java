package dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse;

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
import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.CountPaymentAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CountAggregatorImpl implements CountPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private static final int CURRENT_ONE = 1;

    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final PaymentRepository paymentRepository;
    private final AggregationRepository refundRepository;
    private final AggregationRepository chargebackRepository;

    @Override
    @BasicMetric("count")
    public Integer count(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                paymentRepository::countOperationByFieldWithGroupBy
        );
    }

    @Override
    @BasicMetric("countSuccess")
    public Integer countSuccess(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                checkedField,
                paymentModel,
                timeWindow,
                list,
                paymentRepository::countOperationSuccessWithGroupBy
        );
    }

    @Override
    @BasicMetric("countError")
    public Integer countError(
            PaymentCheckedField checkedField, PaymentModel paymentModel, TimeWindow timeWindow,
            String errorCode, List<PaymentCheckedField> list) {
        try {
            Instant timestamp = paymentModel.getTimestamp() != null
                    ? Instant.ofEpochMilli(paymentModel.getTimestamp())
                    : Instant.now();
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            if (StringUtils.isEmpty(resolve.getValue())) {
                return CURRENT_ONE;
            }
            Integer count = paymentRepository.countOperationErrorWithGroupBy(
                    resolve.getName(),
                    resolve.getValue(),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getEndWindowTime()),
                    eventFields,
                    errorCode
            );

            log.debug(
                    "CountAggregatorImpl field: {} value: {}  countError: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    count
            );
            return count + CURRENT_ONE;
        } catch (Exception e) {
            log.warn("CountAggregatorImpl error when countError e: ", e);
            throw new RuleFunctionException(e);
        }
    }

    @Override
    @BasicMetric("countChargeback")
    public Integer countChargeback(
            PaymentCheckedField paymentCheckedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                paymentCheckedField,
                paymentModel,
                timeWindow,
                list,
                chargebackRepository::countOperationByFieldWithGroupBy,
                false
        );
    }

    @Override
    @BasicMetric("countRefund")
    public Integer countRefund(
            PaymentCheckedField paymentCheckedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                paymentCheckedField,
                paymentModel,
                timeWindow,
                list,
                refundRepository::countOperationByFieldWithGroupBy,
                false
        );
    }

    @NotNull
    private Integer getCount(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Integer> aggregateFunction) {
        return getCount(checkedField, paymentModel, timeWindow, list, aggregateFunction, true);
    }

    @NotNull
    private Integer getCount(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list,
            AggregateGroupingFunction<String, Object, Long, Long, List<FieldModel>, Integer> aggregateFunction,
            boolean withCurrent) {
        try {
            Instant timestamp = paymentModel.getTimestamp() != null
                    ? Instant.ofEpochMilli(paymentModel.getTimestamp())
                    : Instant.now();
            FieldModel resolve = databasePaymentFieldResolver.resolve(checkedField, paymentModel);
            List<FieldModel> eventFields = databasePaymentFieldResolver.resolveListFields(paymentModel, list);

            if (StringUtils.isEmpty(resolve.getValue())) {
                return withCurrent ? CURRENT_ONE : 0;
            }

            Integer count = aggregateFunction.accept(
                    resolve.getName(),
                    resolve.getValue(),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getEndWindowTime()),
                    eventFields
            );

            log.debug(
                    "CountAggregatorImpl field: {} value: {}  count: {}",
                    resolve.getName(),
                    resolve.getValue(),
                    count
            );
            return withCurrent ? count + CURRENT_ONE : count;
        } catch (Exception e) {
            log.warn("CountAggregatorImpl error when getCount e: ", e);
            throw new RuleFunctionException(e);
        }
    }
}
