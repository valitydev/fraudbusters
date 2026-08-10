package dev.vality.fraudbusters.fraud.localstorage.aggregator;

import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.localstorage.LocalPaymentFieldResolver;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorageRepository;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.UniqueValueAggregatorImpl;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.fraudo.aggregator.UniqueValueAggregator;
import dev.vality.fraudo.model.TimeWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LocalUniqueValueAggregatorDecorator implements UniqueValueAggregator<PaymentModel, PaymentCheckedField> {

    private final UniqueValueAggregatorImpl uniqueValueAggregator;
    private final LocalPaymentFieldResolver localPaymentFieldResolver;
    private final LocalResultStorageRepository localStorageRepository;
    private final TimeBoundaryService timeBoundaryService;

    @Override
    public Integer countUniqueValue(
            PaymentCheckedField countField,
            PaymentModel paymentModel,
            PaymentCheckedField onField,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        try {
            Integer uniq = uniqueValueAggregator.countUniqueValue(countField, paymentModel, onField, timeWindow, list);
            Instant timestamp = TimestampUtil.instantFromPaymentModel(paymentModel);
            TimeBound timeBound = timeBoundaryService.getBoundary(timestamp, timeWindow);
            FieldModel resolve = localPaymentFieldResolver.resolve(countField, paymentModel);
            List<FieldModel> fieldModels = localPaymentFieldResolver.resolveListFields(paymentModel, list);
            Integer localUniqCountOperation = localStorageRepository.uniqCountOperationWithGroupBy(
                    resolve.getName(),
                    resolve.getValue(),
                    localPaymentFieldResolver.resolveName(onField),
                    timeBound.getLeft().getEpochSecond(),
                    timeBound.getRight().getEpochSecond(),
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
