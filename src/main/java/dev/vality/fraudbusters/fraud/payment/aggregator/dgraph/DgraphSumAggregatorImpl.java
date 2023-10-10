package dev.vality.fraudbusters.fraud.payment.aggregator.dgraph;

import dev.vality.fraudbusters.aspect.BasicMetric;
import dev.vality.fraudbusters.constant.ChargebackStatus;
import dev.vality.fraudbusters.constant.PaymentStatus;
import dev.vality.fraudbusters.constant.RefundStatus;
import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudbusters.fraud.constant.DgraphEntity;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphAggregationQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphEntityResolver;
import dev.vality.fraudbusters.repository.DgraphAggregatesRepository;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.SumPaymentAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.createFiltersList;
import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.getTimestamp;

@Slf4j
@RequiredArgsConstructor
public class DgraphSumAggregatorImpl implements SumPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private final DgraphAggregationQueryBuilderService dgraphSumQueryBuilderService;
    private final DgraphEntityResolver dgraphEntityResolver;
    private final DgraphAggregatesRepository dgraphAggregatesRepository;
    private final TimeBoundaryService timeBoundaryService;

    @Override
    @BasicMetric("sum")
    public Double sum(PaymentCheckedField checkedField,
                      PaymentModel model,
                      TimeWindow timeWindow,
                      List<PaymentCheckedField> fields) {
        return getSum(checkedField, model, timeWindow, fields, DgraphEntity.PAYMENT, null);
    }

    @Override
    @BasicMetric("sumSuccess")
    public Double sumSuccess(PaymentCheckedField checkedField,
                             PaymentModel model,
                             TimeWindow timeWindow,
                             List<PaymentCheckedField> fields) {
        return getSum(checkedField, model, timeWindow, fields, DgraphEntity.PAYMENT, PaymentStatus.captured.name());
    }

    @Override
    @BasicMetric("sumError")
    public Double sumError(PaymentCheckedField checkedField,
                           PaymentModel model,
                           TimeWindow timeWindow,
                           String errorCode,
                           List<PaymentCheckedField> fields) {
        return getSum(checkedField, model, timeWindow, fields, DgraphEntity.PAYMENT, PaymentStatus.failed.name());
    }

    @Override
    @BasicMetric("sumError")
    public Double sumError(PaymentCheckedField checkedField, PaymentModel model, TimeWindow timeWindow,
                           List<PaymentCheckedField> fields) {
        return getSum(checkedField, model, timeWindow, fields, DgraphEntity.PAYMENT, PaymentStatus.failed.name());
    }

    @Override
    public Double sumChargeback(PaymentCheckedField checkedField,
                                PaymentModel model,
                                TimeWindow timeWindow,
                                List<PaymentCheckedField> fields) {
        return getSum(
                checkedField, model, timeWindow, fields, DgraphEntity.CHARGEBACK, ChargebackStatus.accepted.name()
        );
    }

    @Override
    public Double sumRefund(PaymentCheckedField checkedField,
                            PaymentModel model,
                            TimeWindow timeWindow,
                            List<PaymentCheckedField> fields) {
        return getSum(checkedField, model, timeWindow, fields, DgraphEntity.REFUND, RefundStatus.succeeded.name());
    }

    private Double getSum(PaymentCheckedField checkedField,
                          PaymentModel paymentModel,
                          TimeWindow timeWindow,
                          List<PaymentCheckedField> fields,
                          DgraphEntity targetEntity,
                          String status) {
        Instant timestamp = getTimestamp(paymentModel);
        TimeBound timeBound = timeBoundaryService.getBoundary(timestamp, timeWindow);
        List<PaymentCheckedField> filters = createFiltersList(checkedField, fields);
        String countQuery = dgraphSumQueryBuilderService.getQuery(
                dgraphEntityResolver.resolvePaymentCheckedField(checkedField),
                targetEntity,
                dgraphEntityResolver.resolvePaymentCheckedFieldsToMap(filters),
                paymentModel,
                timeBound.getLeft(),
                timeBound.getRight(),
                status
        );
        return dgraphAggregatesRepository.getSum(countQuery);
    }

}
