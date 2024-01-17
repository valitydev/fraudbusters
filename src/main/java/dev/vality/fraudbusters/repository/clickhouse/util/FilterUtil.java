package dev.vality.fraudbusters.repository.clickhouse.util;


import dev.vality.fraudbusters.constant.QueryParamName;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.util.CompositeIdUtil;
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

    private static final String PAGE_CONTENT_COMPOSITE_FILTER = " and (id %s :id or (status != :status and id = :id)) ";
    private static final String PAGE_CONTENT_FILTER = " and (id %s :id ) ";

    public static String appendFilters(FilterDto filter) {
        StringBuilder filters = new StringBuilder();
        Set<SearchFieldDto> searchFields = filter.getSearchFields();
        if (!CollectionUtils.isEmpty(searchFields)) {
            addLikeSearchFields(filters, searchFields);
            addEqualSearchFields(filters, searchFields);
        }
        if (Objects.nonNull(filter.getLastId())) {
            String pageFilter = CompositeIdUtil.isComposite(filter.getLastId())
                    ? PAGE_CONTENT_COMPOSITE_FILTER
                    : PAGE_CONTENT_FILTER;
            if (SortOrder.DESC.equals(filter.getSort().getOrder())) {
                filters.append(String.format(pageFilter, "<"));
            } else {
                filters.append(String.format(pageFilter, ">"));
            }
        }
        String sorting = String.format(" ORDER BY (eventTime, id) %s ", filter.getSort().getOrder().name());
        String limit = " LIMIT :size ";
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
            if (CompositeIdUtil.isComposite(filter.getLastId())) {
                List<String> compositeId = CompositeIdUtil.extract(filter.getLastId());
                if (compositeId.size() == 2) {
                    params.addValue(QueryParamName.ID, compositeId.get(0))
                            .addValue(QueryParamName.EVENT_TIME, compositeId.get(1));
                }
            } else {
                params.addValue(QueryParamName.ID, filter.getLastId());
            }
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
