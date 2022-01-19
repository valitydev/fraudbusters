package dev.vality.fraudbusters.fraud.payment.aggregator.dgraph.query.builder;

import dev.vality.fraudbusters.fraud.constant.DgraphEntity;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface DgraphAggregationQueryBuilderService {

    String getQuery(DgraphEntity rootEntity,
                    DgraphEntity targetEntity,
                    Map<DgraphEntity, Set<PaymentCheckedField>> dgraphEntityMap,
                    PaymentModel paymentModel,
                    Instant startWindowTime,
                    Instant endWindowTime,
                    String status);

}
