package dev.vality.fraudbusters.repository;

import dev.vality.fraudbusters.fraud.model.FieldModel;

import java.util.List;

public interface PaymentRepository extends AggregationRepository {

    Integer countOperationSuccessWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Integer countOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            String errorCode);

    Integer countOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Long sumOperationSuccessWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

    Long sumOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels,
            String errorCode);

    Long sumOperationErrorWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels);

}
