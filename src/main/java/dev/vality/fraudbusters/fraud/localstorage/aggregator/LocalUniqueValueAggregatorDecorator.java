package dev.vality.fraudbusters.fraud.localstorage.aggregator;

import com.rbkmoney.fraudo.aggregator.UniqueValueAggregator;
import com.rbkmoney.fraudo.model.TimeWindow;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.UniqueValueAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LocalUniqueValueAggregatorDecorator implements UniqueValueAggregator<PaymentModel, PaymentCheckedField> {

    private final UniqueValueAggregatorImpl uniqueValueAggregator;
    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final LocalResultStorageRepository localStorageRepository;

    @Override
    public Integer countUniqueValue(
            PaymentCheckedField countField,
            PaymentModel paymentModel,
            PaymentCheckedField onField,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        try {
            Integer uniq = uniqueValueAggregator.countUniqueValue(countField, paymentModel, onField, timeWindow, list);
            Instant now = TimestampUtil.instantFromPaymentModel(paymentModel);
            FieldModel resolve = databasePaymentFieldResolver.resolve(countField, paymentModel);
            List<FieldModel> fieldModels = databasePaymentFieldResolver.resolveListFields(paymentModel, list);
            Integer localUniqCountOperation = localStorageRepository.uniqCountOperationWithGroupBy(
                    resolve.getName(),
                    resolve.getValue(),
                    databasePaymentFieldResolver.resolve(onField),
                    TimestampUtil.generateTimestampMinusMinutesMillis(now, timeWindow.getStartWindowTime()),
                    TimestampUtil.generateTimestampMinusMinutesMillis(now, timeWindow.getEndWindowTime()),
                    fieldModels
            );
            int result = localUniqCountOperation + uniq;
            log.debug("LocalUniqueValueAggregatorDecorator countUniqueValue: {}", result);
            return result;
        } catch (Exception e) {
            log.warn("LocalUniqueValueAggregatorDecorator error when getCount e: ", e);
            throw new RuleFunctionException(e);
        }
    }
}
