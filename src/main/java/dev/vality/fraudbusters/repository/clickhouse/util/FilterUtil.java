package dev.vality.fraudbusters.repository.clickhouse.util;


import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.constant.QueryParamName;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterUtil {

    public static String appendFilters(FilterDto filter) {
        return appendFilters(filter, false);
    }

    public static String appendFraudResultFilters(FilterDto filter) {
        return appendFilters(filter, true);
    }

    private static String appendFilters(FilterDto filter, boolean directFraudResultSearch) {
        StringBuilder filters = new StringBuilder();
        Set<SearchFieldDto> searchFields = filter.getSearchFields();
        if (!CollectionUtils.isEmpty(searchFields)) {
            addLikeSearchFields(filters, searchFields);
            addEqualSearchFields(filters, searchFields);
            if (directFraudResultSearch) {
                addDirectFraudResultSearchFields(filters, searchFields);
            } else {
                addFraudResultSearchFields(filters, searchFields);
            }
        }
        String sorting = String.format(" ORDER BY (eventTime, id) %s ", filter.getSort().getOrder().name());
        String limit = " LIMIT :size ";
        if (Objects.nonNull(filter.getLastId())) {
            limit = " LIMIT :size OFFSET :offset";
        }

        return filters.append(sorting).append(limit).toString();
    }

    private static void addLikeSearchFields(StringBuilder filters, Set<SearchFieldDto> searchFields) {
        searchFields.stream()
                .filter(searchField -> searchField.getType().equals(FieldType.STRING))
                .forEach(searchField ->
                        filters
                                .append(" and like(")
                                .append(searchField.getField().getValue())
                                .append(",'")
                                .append(searchField.getValue())
                                .append("')"));
    }

    private static void addEqualSearchFields(StringBuilder filters, Set<SearchFieldDto> searchFields) {
        searchFields.stream()
                .filter(searchField -> searchField.getType().equals(FieldType.ENUM))
                .forEach(searchField ->
                        filters
                                .append(" and ")
                                .append(searchField.getField().getValue())
                                .append(" = '")
                                .append(searchField.getValue())
                                .append("'"));
    }

    private static void addFraudResultSearchFields(StringBuilder filters, Set<SearchFieldDto> searchFields) {
        List<SearchFieldDto> fraudResultFields = searchFields.stream()
                .filter(searchField -> searchField.getType().equals(FieldType.FRAUD_RESULT))
                .toList();
        if (fraudResultFields.isEmpty()) {
            return;
        }
        filters.append(" and id in (select id from ")
                .append(EventSource.FRAUD_EVENTS_UNIQUE.getTable())
                .append(" where timestamp >= toDate(:from)")
                .append(" and timestamp <= toDate(:to)")
                .append(" and toDateTime(eventTime) >= toDateTime(:from)")
                .append(" and toDateTime(eventTime) <= toDateTime(:to)");
        fraudResultFields.forEach(searchField -> filters
                .append(" and ")
                .append(searchField.getField().getValue())
                .append(" = :")
                .append(searchField.getField().getValue()));
        filters.append(")");
    }

    private static void addDirectFraudResultSearchFields(StringBuilder filters, Set<SearchFieldDto> searchFields) {
        searchFields.stream()
                .filter(searchField -> searchField.getType().equals(FieldType.FRAUD_RESULT))
                .forEach(searchField -> filters
                        .append(" and ")
                        .append(searchField.getField().getValue())
                        .append(" = :")
                        .append(searchField.getField().getValue()));
    }

    public static MapSqlParameterSource initParams(FilterDto filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (Objects.nonNull(filter.getLastId())) {
            params.addValue(QueryParamName.OFFSET, Integer.valueOf(filter.getLastId()));
        }
        return addTimeParams(params, filter);
    }

    @NotNull
    private static MapSqlParameterSource addTimeParams(MapSqlParameterSource params, FilterDto filter) {
        params.addValue(QueryParamName.FROM, filter.getTimeFrom())
                .addValue(QueryParamName.TO, filter.getTimeTo())
                .addValue(QueryParamName.SIZE, filter.getSize());
        if (!CollectionUtils.isEmpty(filter.getSearchFields())) {
            filter.getSearchFields().stream()
                    .filter(searchField -> searchField.getType().equals(FieldType.FRAUD_RESULT))
                    .forEach(searchField -> params.addValue(
                            searchField.getField().getValue(),
                            searchField.getValue()));
        }
        return params;
    }

}
