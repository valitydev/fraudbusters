package dev.vality.fraudbusters.repository;

import dev.vality.fraudbusters.fraud.model.FieldModel;

import java.util.List;

public interface AdditionalAggrRepository {

    Integer countRefundOperationByFieldWithGroupBy(
            String fieldName,
            String value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Integer countChargebackOperationByFieldWithGroupBy(
            String fieldName,
            String value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Long sumRefundOperationByFieldWithGroupBy(
            String fieldName,
            String value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Long sumChargebackOperationByFieldWithGroupBy(
            String fieldName,
            String value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

}
