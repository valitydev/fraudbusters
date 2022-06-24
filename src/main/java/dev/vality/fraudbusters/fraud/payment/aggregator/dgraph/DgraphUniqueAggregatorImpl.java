package dev.vality.fraudbusters.fraud.payment.aggregator.dgraph;

import dev.vality.fraudbusters.constant.PaymentStatus;
import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphAggregationQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphEntityResolver;
import dev.vality.fraudbusters.repository.DgraphAggregatesRepository;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudo.aggregator.UniqueValueAggregator;
import dev.vality.fraudo.model.TimeWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.createFiltersList;
import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.getTimestamp;

@Slf4j
@RequiredArgsConstructor
public class DgraphUniqueAggregatorImpl implements UniqueValueAggregator<PaymentModel, PaymentCheckedField> {

    private final DgraphAggregationQueryBuilderService dgraphUniqueQueryBuilderService;
    private final DgraphEntityResolver dgraphEntityResolver;
    private final DgraphAggregatesRepository dgraphAggregatesRepository;
    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;
    private final TimeBoundaryService timeBoundaryService;

    private static final int CURRENT_ONE = 1;

    @Override
    public Integer countUniqueValue(PaymentCheckedField countField,
                                    PaymentModel paymentModel,
                                    PaymentCheckedField onField,
                                    TimeWindow timeWindow,
                                    List<PaymentCheckedField> fields) {
        FieldModel resolve = databasePaymentFieldResolver.resolve(countField, paymentModel);
        if (Objects.isNull(resolve.getValue())) {
            return CURRENT_ONE;
        }

        if (onField == PaymentCheckedField.MOBILE || onField == PaymentCheckedField.RECURRENT) {
            return 0; //TODO: реализовать подсчет
        }

        Instant timestamp = getTimestamp(paymentModel);
        TimeBound timeBound = timeBoundaryService.getBoundary(timestamp, timeWindow);
        List<PaymentCheckedField> filters = createFiltersList(countField, fields);
        String countQuery = dgraphUniqueQueryBuilderService.getQuery(
                dgraphEntityResolver.resolvePaymentCheckedField(countField),
                dgraphEntityResolver.resolvePaymentCheckedField(onField),
                dgraphEntityResolver.resolvePaymentCheckedFieldsToMap(filters),
                paymentModel,
                timeBound.getLeft(),
                timeBound.getRight(),
                PaymentStatus.captured.name()
        );
        return dgraphAggregatesRepository.getCount(countQuery) + CURRENT_ONE;
    }

}
