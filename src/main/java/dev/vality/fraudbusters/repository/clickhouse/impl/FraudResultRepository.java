package dev.vality.fraudbusters.repository.clickhouse.impl;

import com.google.common.collect.Lists;
import dev.vality.fraudbusters.constant.ClickhouseSchemeNames;
import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.domain.Event;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.repository.AggregationGeneralRepository;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.extractor.CountExtractor;
import dev.vality.fraudbusters.repository.clickhouse.extractor.SumExtractor;
import dev.vality.fraudbusters.repository.clickhouse.impl.generator.EventParametersGenerator;
import dev.vality.fraudbusters.repository.clickhouse.mapper.EventMapper;
import dev.vality.fraudbusters.repository.clickhouse.query.FraudResultQuery;
import dev.vality.fraudbusters.repository.clickhouse.setter.EventBatchPreparedStatementSetter;
import dev.vality.fraudbusters.repository.clickhouse.util.AggregationUtil;
import dev.vality.fraudbusters.repository.clickhouse.util.FilterUtil;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudResultRepository implements Repository<Event>, PaymentRepository {

    private final AggregationGeneralRepository aggregationGeneralRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final EventMapper eventMapper;

    @Override
    public void insert(Event value) {
        log.debug("EventRepository insert value: {}", value);
        if (value != null) {
            Map<String, Object> parameters = EventParametersGenerator.generateParamsByFraudModel(value);
            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getDataSource())
                    .withSchemaName(ClickhouseSchemeNames.FRAUD)
                    .withTableName(ClickhouseSchemeNames.EVENTS_UNIQUE);
            simpleJdbcInsert.setColumnNames(Lists.newArrayList(parameters.keySet()));
            simpleJdbcInsert.execute(parameters);
        }
    }

    @Override
    public void insertBatch(List<Event> events) {
        log.debug("EventRepository insertBatch events: {}", events);
        if (events != null && !events.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    EventBatchPreparedStatementSetter.INSERT,
                    new EventBatchPreparedStatementSetter(events)
            );
        }
    }

    @Override
    public List<Event> getByFilter(FilterDto filter) {
        String filters = FilterUtil.appendFilters(filter);
        String query = FraudResultQuery.SELECT_HISTORY_FRAUD_RESULT + filters;
        MapSqlParameterSource params = FilterUtil.initParams(filter);
        return namedParameterJdbcTemplate.query(query, params, eventMapper);
    }

    @Override
    public Integer countOperationByField(String fieldName, Object value, Long from, Long to) {
        return aggregationGeneralRepository.countOperationByField(
                EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                fieldName,
                value,
                from,
                to
        );
    }

    @Override
    public Integer countOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        return aggregationGeneralRepository.countOperationByFieldWithGroupBy(EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                fieldName,
                value,
                from,
                to,
                fieldModels
        );
    }

    @Override
    public Long sumOperationByFieldWithGroupBy(
            String fieldName,
            Object value,
            Long from,
            Long to,
            List<FieldModel> fieldModels) {
        return aggregationGeneralRepository.sumOperationByFieldWithGroupBy(EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                fieldName,
                value,
                from,
                to,
                fieldModels
        );
    }

    @Override
    public Integer uniqCountOperation(String fieldNameBy, Object value, String fieldNameCount, Long from, Long to) {
        return aggregationGeneralRepository.uniqCountOperation(EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                fieldNameBy,
                value,
                fieldNameCount,
                from,
                to
        );
    }

    @Override
    public Integer uniqCountOperationWithGroupBy(
            String fieldNameBy, Object value, String fieldNameCount, Long from,
            Long to, List<FieldModel> fieldModels) {
        return aggregationGeneralRepository.uniqCountOperationWithGroupBy(EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                fieldNameBy,
                value,
                fieldNameCount,
                from,
                to,
                fieldModels
        );
    }

    @Override
    public Integer countOperationSuccessWithGroupBy(
            String fieldName, Object value, Long from, Long to,
            List<FieldModel> fieldModels) {
        StringBuilder sql = new StringBuilder(String.format("""
                        select %1$s, count() as cnt
                        from %2$s
                        where timestamp >= ?
                        and timestamp <= ?
                        and eventTime >= ?
                        and eventTime <= ?
                        and %1$s = ? and resultStatus != ?""",
                fieldName,
                EventSource.FRAUD_EVENTS_UNIQUE.getTable()
        ));
        StringBuilder sqlGroupBy = new StringBuilder(String.format("group by %1$s", fieldName));
        StringBuilder resultSql = AggregationUtil.appendGroupingFields(fieldModels, sql, sqlGroupBy);
        List<Object> params = AggregationUtil.generateParams(from, to, fieldModels, value, ResultStatus.DECLINE.name());
        log.debug("FraudResultRepository countOperationSuccessWithGroupBy sql: {} params: {}", sql, params);
        return jdbcTemplate.query(resultSql.toString(), params.toArray(), new CountExtractor());
    }

    @Override
    public Integer countOperationErrorWithGroupBy(
            String fieldName, Object value, Long from, Long to,
            List<FieldModel> fieldModels, String errorCode) {
        log.warn(
                "Error code ignore on this source: {} errorCode: {}",
                EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                errorCode
        );
        StringBuilder sql = new StringBuilder(String.format("""
                        select %1$s, count() as cnt
                        from %2$s
                        where timestamp >= ?
                        and timestamp <= ?
                        and eventTime >= ?
                        and eventTime <= ?
                        and %1$s = ? and resultStatus = ?""",
                fieldName,
                EventSource.FRAUD_EVENTS_UNIQUE.getTable()
        ));
        StringBuilder sqlGroupBy = new StringBuilder(String.format("group by %1$s", fieldName));
        StringBuilder resultSql = AggregationUtil.appendGroupingFields(fieldModels, sql, sqlGroupBy);
        List<Object> params = AggregationUtil.generateParams(from, to, fieldModels, value, ResultStatus.DECLINE.name());
        log.debug("FraudResultRepository countOperationErrorWithGroupBy sql: {} params: {}", sql, params);
        return jdbcTemplate.query(resultSql.toString(), params.toArray(), new CountExtractor());
    }

    @Override
    public Integer countOperationErrorWithGroupBy(String fieldName,
                                                  Object value,
                                                  Long from,
                                                  Long to,
                                                  List<FieldModel> fieldModels) {
        return countOperationErrorWithGroupBy(fieldName, value, from, to, fieldModels, null);
    }

    @Override
    public Long sumOperationSuccessWithGroupBy(
            String fieldName, Object value, Long from, Long to,
            List<FieldModel> fieldModels) {
        StringBuilder sql = new StringBuilder(String.format("""
                        select %1$s, sum(amount) as sum
                        from %2$s
                        where timestamp >= ?
                        and timestamp <= ?
                        and eventTime >= ?
                        and eventTime <= ?
                        and %1$s = ? and resultStatus != ?""",
                fieldName,
                EventSource.FRAUD_EVENTS_UNIQUE.getTable()
        ));
        StringBuilder sqlGroupBy = new StringBuilder(String.format("group by %1$s", fieldName));
        StringBuilder resultSql = AggregationUtil.appendGroupingFields(fieldModels, sql, sqlGroupBy);
        List<Object> params = AggregationUtil.generateParams(from, to, fieldModels, value, ResultStatus.DECLINE.name());
        log.debug("FraudResultRepository sumOperationSuccessWithGroupBy sql: {} params: {}", sql, params);
        return jdbcTemplate.query(resultSql.toString(), params.toArray(), new SumExtractor());
    }

    @Override
    public Long sumOperationErrorWithGroupBy(
            String fieldName, Object value, Long from, Long to,
            List<FieldModel> fieldModels, String errorCode) {
        log.warn(
                "Error code ignore on this source: {} errorCode: {}",
                EventSource.FRAUD_EVENTS_UNIQUE.getTable(),
                errorCode
        );
        StringBuilder sql = new StringBuilder(String.format("""
                        select %1$s, sum(amount) as sum
                        from %2$s
                        where timestamp >= ?
                        and timestamp <= ?
                        and eventTime >= ?
                        and eventTime <= ?
                        and %1$s = ? and resultStatus = ?""",
                fieldName,
                EventSource.FRAUD_EVENTS_UNIQUE.getTable()
        ));
        StringBuilder sqlGroupBy = new StringBuilder(String.format("group by %1$s", fieldName));
        StringBuilder resultSql = AggregationUtil.appendGroupingFields(fieldModels, sql, sqlGroupBy);
        List<Object> params = AggregationUtil.generateParams(from, to, fieldModels, value, ResultStatus.DECLINE.name());
        log.debug("FraudResultRepository sumOperationErrorWithGroupBy sql: {} params: {}", sql, params);
        return jdbcTemplate.query(resultSql.toString(), params.toArray(), new SumExtractor());
    }

    @Override
    public Long sumOperationErrorWithGroupBy(String fieldName, Object value, Long from, Long to,
                                             List<FieldModel> fieldModels) {
        return sumOperationErrorWithGroupBy(fieldName, value, from, to, fieldModels, null);
    }

}
