package dev.vality.fraudbusters.fraud.payment.aggregator.dgraph;

import dev.vality.fraudbusters.aspect.BasicMetric;
import dev.vality.fraudbusters.constant.ChargebackStatus;
import dev.vality.fraudbusters.constant.PaymentStatus;
import dev.vality.fraudbusters.constant.RefundStatus;
import dev.vality.fraudbusters.fraud.constant.DgraphEntity;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder.DgraphAggregationQueryBuilderService;
import dev.vality.fraudbusters.fraud.payment.resolver.DgraphEntityResolver;
import dev.vality.fraudbusters.repository.DgraphAggregatesRepository;
import dev.vality.fraudo.model.TimeWindow;
import dev.vality.fraudo.payment.aggregator.CountPaymentAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.createFiltersList;
import static dev.vality.fraudbusters.util.DgraphAggregatorUtils.getTimestamp;

@Slf4j
@RequiredArgsConstructor
public class DgraphCountAggregatorImpl implements CountPaymentAggregator<PaymentModel, PaymentCheckedField> {

    private final DgraphAggregationQueryBuilderService dgraphCountQueryBuilderService;
    private final DgraphEntityResolver dgraphEntityResolver;
    private final DgraphAggregatesRepository dgraphAggregatesRepository;

    @Override
    @BasicMetric("count")
    public Integer count(PaymentCheckedField checkedField,
                         PaymentModel paymentModel,
                         TimeWindow timeWindow,
                         List<PaymentCheckedField> list) {
        return getCount(checkedField, paymentModel, timeWindow, list, DgraphEntity.PAYMENT, null);
    }

    @Override
    @BasicMetric("countSuccess")
    public Integer countSuccess(PaymentCheckedField checkedField,
                                PaymentModel paymentModel,
                                TimeWindow timeWindow,
                                List<PaymentCheckedField> list) {
        return getCount(
                checkedField, paymentModel, timeWindow, list, DgraphEntity.PAYMENT, PaymentStatus.captured.name()
        );
    }

    @Override
    @BasicMetric("countError")
    public Integer countError(PaymentCheckedField checkedField,
                              PaymentModel paymentModel,
                              TimeWindow timeWindow,
                              String errorCode,
                              List<PaymentCheckedField> list) {
        return getCount(
                checkedField, paymentModel, timeWindow, list, DgraphEntity.PAYMENT, PaymentStatus.failed.name()
        );
    }

    @Override
    @BasicMetric("countChargeback")
    public Integer countChargeback(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                checkedField, paymentModel, timeWindow, list, DgraphEntity.CHARGEBACK, ChargebackStatus.accepted.name()
        );
    }

    @Override
    @BasicMetric("countRefund")
    public Integer countRefund(
            PaymentCheckedField checkedField,
            PaymentModel paymentModel,
            TimeWindow timeWindow,
            List<PaymentCheckedField> list) {
        return getCount(
                checkedField, paymentModel, timeWindow, list, DgraphEntity.REFUND, RefundStatus.succeeded.name()
        );
    }

    private Integer getCount(PaymentCheckedField checkedField,
                             PaymentModel paymentModel,
                             TimeWindow timeWindow,
                             List<PaymentCheckedField> fields,
                             DgraphEntity targetEntity,
                             String status) {
        Instant timestamp = getTimestamp(paymentModel);
        List<PaymentCheckedField> filters = createFiltersList(checkedField, fields);

        String countQuery = dgraphCountQueryBuilderService.getQuery(
                dgraphEntityResolver.resolvePaymentCheckedField(checkedField),
                targetEntity,
                dgraphEntityResolver.resolvePaymentCheckedFieldsToMap(filters),
                paymentModel,
                timestamp.minus(timeWindow.getStartWindowTime(), timeWindow.getTimeUnit()),
                timestamp.minus(timeWindow.getEndWindowTime(), timeWindow.getTimeUnit()),
                status
        );
        return dgraphAggregatesRepository.getCount(countQuery);
    }

}
