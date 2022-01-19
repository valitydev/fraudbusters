package dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse;

import com.rbkmoney.fraudo.aggregator.UniqueValueAggregator;
import com.rbkmoney.fraudo.model.TimeWindow;
import dev.vality.fraudbusters.aspect.BasicMetric;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UniqueValueAggregatorImpl implements UniqueValueAggregator<PaymentModel, PaymentCheckedField> {

    private static final int CURRENT_ONE = 1;

    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final PaymentRepository paymentRepository;

    @Override
    @BasicMetric("countUniqueValueWindowed")
    public Integer countUniqueValue(
            PaymentCheckedField countField,
            PaymentModel paymentModel,
            PaymentCheckedField onField,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        try {
            Instant timestamp = paymentModel.getTimestamp() != null
                    ? Instant.ofEpochMilli(paymentModel.getTimestamp())
                    : Instant.now();
            FieldModel resolve = databasePaymentFieldResolver.resolve(countField, paymentModel);
            if (StringUtils.isEmpty(resolve.getValue())) {
                return CURRENT_ONE;
            }
            List<FieldModel> fieldModels = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Integer uniqCountOperation = paymentRepository.uniqCountOperationWithGroupBy(
                    resolve.getName(),
                    resolve.getValue(),
                    databasePaymentFieldResolver.resolve(onField),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(timestamp, timeWindow.getEndWindowTime()),
                    fieldModels
            );
            return uniqCountOperation + CURRENT_ONE;
        } catch (Exception e) {
            log.warn("UniqueValueAggregatorImpl error when getCount e: ", e);
            throw new RuleFunctionException(e);
        }
    }

}
