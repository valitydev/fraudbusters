package dev.vality.fraudbusters.repository.clickhouse.util;


import dev.vality.fraudbusters.constant.QueryParamName;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterUtil {

    public static String appendFilters(FilterDto filter) {
        StringBuilder filters = new StringBuilder();
        Set<SearchFieldDto> searchFields = filter.getSearchFields();
        if (!CollectionUtils.isEmpty(searchFields)) {
            addLikeSearchFields(filters, searchFields);
            addEqualSearchFields(filters, searchFields);
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
        return params;
    }

}
