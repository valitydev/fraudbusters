package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.constant.RefundStatus;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.repository.AggregationRepository;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.FraudPaymentRowMapper;
import dev.vality.fraudbusters.repository.clickhouse.query.FraudPaymentQuery;
import dev.vality.fraudbusters.repository.clickhouse.setter.FraudPaymentBatchPreparedStatementSetter;
import dev.vality.fraudbusters.repository.clickhouse.util.FilterUtil;
import dev.vality.fraudbusters.service.dto.FilterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudPaymentRepository implements Repository<FraudPaymentRow>, AggregationRepository {

    private static final String INSERT = String.format("""
                    INSERT INTO
                    %s
                     (timestamp,
                     eventTimeHour,
                     eventTime,
                     id,
                     fraudType ,
                     comment,
                     email,
                     phone,
                     ip,
                     fingerprint,
                     bin,
                     maskedPan,
                     cardToken,
                     cardCategory,
                     paymentSystem,
                     paymentTool,
                     terminal,
                     providerId,
                     bankCountry,
                     partyId,
                     shopId,
                     amount,
                     currency,
                     status,
                     errorReason,
                     errorCode,
                     paymentCountry)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable());
    private final AggregationStatusGeneralRepositoryImpl aggregationStatusGeneralRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final FraudPaymentRowMapper fraudPaymentRowMapper;

    @Override
    public void insert(FraudPaymentRow value) {
        throw new UnsupportedOperationException("Method insertBatch is not support!");
    }

    @Override
    public void insertBatch(List<FraudPaymentRow> payments) {
        log.debug("FraudPaymentRepository insertBatch payments: {}", payments);
        if (!CollectionUtils.isEmpty(payments)) {
            jdbcTemplate.batchUpdate(INSERT, new FraudPaymentBatchPreparedStatementSetter(payments));
        }
    }

    @Override
    public List<FraudPaymentRow> getByFilter(FilterDto filter) {
        String filters = FilterUtil.appendFilters(filter);
        String query = FraudPaymentQuery.SELECT_HISTORY_FRAUD_PAYMENT + filters;
        MapSqlParameterSource params = FilterUtil.initParams(filter);
        return namedParameterJdbcTemplate.query(query, params, fraudPaymentRowMapper);
    }

    @Override
    public Integer countOperationByField(String fieldName, Object value, Long from, Long to) {
        return aggregationStatusGeneralRepository.countOperationByField(
                EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable(),
                fieldName,
                value,
                from,
                to,
                RefundStatus.succeeded.name()
        );
    }

    @Override
    public Integer countOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        return aggregationStatusGeneralRepository.countOperationByFieldWithGroupBy(
                EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable(),
                fieldName,
                value,
                from,
                to,
                fieldModels,
                RefundStatus.succeeded.name()
        );
    }

    @Override
    public Long sumOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        return aggregationStatusGeneralRepository.sumOperationByFieldWithGroupBy(
                EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable(),
                fieldName,
                value,
                from,
                to,
                fieldModels,
                RefundStatus.succeeded.name()
        );
    }

    @Override
    public Integer uniqCountOperation(String fieldNameBy, Object value, String fieldNameCount, Long from, Long to) {
        return aggregationStatusGeneralRepository.uniqCountOperation(
                EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable(),
                fieldNameBy,
                value,
                fieldNameCount,
                from,
                to,
                RefundStatus.succeeded.name()
        );
    }

    @Override
    public Integer uniqCountOperationWithGroupBy(
            String fieldNameBy, Object value, String fieldNameCount, Long from,
            Long to, List<FieldModel> fieldModels) {
        return aggregationStatusGeneralRepository.uniqCountOperationWithGroupBy(
                EventSource.FRAUD_EVENTS_FRAUD_PAYMENT.getTable(),
                fieldNameBy,
                value,
                fieldNameCount,
                from,
                to,
                fieldModels,
                RefundStatus.succeeded.name()
        );
    }

}
