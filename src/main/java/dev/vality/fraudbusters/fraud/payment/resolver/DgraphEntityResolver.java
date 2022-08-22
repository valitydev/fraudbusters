package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudbusters.exception.UnknownFieldException;
import dev.vality.fraudbusters.fraud.constant.DgraphEntity;
import dev.vality.fraudbusters.fraud.constant.DgraphTargetAggregationType;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DgraphEntityResolver {

    public Map<DgraphEntity, Set<PaymentCheckedField>> resolvePaymentCheckedFieldsToMap(
            List<PaymentCheckedField> fields
    ) {
        if (fields == null || fields.isEmpty()) {
            return new HashMap<>();
        }
        Map<DgraphEntity, Set<PaymentCheckedField>> entityMap = new HashMap<>();
        for (PaymentCheckedField field : fields) {
            DgraphEntity dgraphEntity = resolvePaymentCheckedField(field);
            if (entityMap.containsKey(dgraphEntity)) {
                entityMap.get(dgraphEntity).add(field);
            } else {
                Set<PaymentCheckedField> paymentCheckedFields = new HashSet<>();
                paymentCheckedFields.add(field);
                entityMap.put(dgraphEntity, paymentCheckedFields);
            }
        }
        return entityMap;
    }

    public DgraphEntity resolvePaymentCheckedField(PaymentCheckedField field) {
        if (field == null) {
            throw new UnknownFieldException();
        }
        return switch (field) {
            case CARD_TOKEN, LAST_DIGITS -> DgraphEntity.TOKEN;
            case PARTY_ID -> DgraphEntity.PARTY;
            case SHOP_ID -> DgraphEntity.SHOP;
            case FINGERPRINT -> DgraphEntity.FINGERPRINT;
            case EMAIL -> DgraphEntity.EMAIL;
            case IP, COUNTRY_IP -> DgraphEntity.IP;
            case BIN -> DgraphEntity.BIN;
            case CURRENCY -> DgraphEntity.CURRENCY;
            case COUNTRY_BANK -> DgraphEntity.COUNTRY;
            case RECURRENT, MOBILE -> DgraphEntity.PAYMENT;
            default -> throw new UnknownFieldException();
        };
    }

    public DgraphEntity resolveDgraphEntityByTargetAggregationType(DgraphTargetAggregationType type) {
        return switch (type) {
            case PAYMENT -> DgraphEntity.PAYMENT;
            case REFUND -> DgraphEntity.REFUND;
            case CHARGEBACK -> DgraphEntity.CHARGEBACK;
            default -> throw new UnsupportedOperationException(String.format("Unknown type %s", type));
        };
    }

    public DgraphTargetAggregationType resolveDgraphTargetAggregationType(DgraphEntity entity) {
        return switch (entity) {
            case PAYMENT -> DgraphTargetAggregationType.PAYMENT;
            case REFUND -> DgraphTargetAggregationType.REFUND;
            case CHARGEBACK -> DgraphTargetAggregationType.CHARGEBACK;
            default -> throw new UnsupportedOperationException(String.format("Unknown entity %s", entity));
        };
    }

}
