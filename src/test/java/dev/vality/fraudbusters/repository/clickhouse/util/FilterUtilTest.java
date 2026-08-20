package dev.vality.fraudbusters.repository.clickhouse.util;

import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterUtilTest {

    @Test
    void combinesEmailTemplateAndRuleFilters() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T00:00:00");
        filter.setTimeTo("2020-05-31T23:59:59");
        SortDto sort = new SortDto();
        sort.setOrder(SortOrder.DESC);
        filter.setSort(sort);
        filter.setSearchFields(Set.of(
                searchField(PaymentField.EMAIL, FieldType.STRING, "test@example.com"),
                searchField(PaymentField.CHECKED_TEMPLATE, FieldType.FRAUD_RESULT, "shop-template"),
                searchField(PaymentField.CHECKED_RULE, FieldType.FRAUD_RESULT, "many_emails_per_card")
        ));

        String sql = FilterUtil.appendPaymentFilters(filter);

        assertTrue(sql.contains("like(email,'test@example.com')"));
        assertTrue(sql.contains("id in ("));
        assertTrue(sql.contains("SELECT id"));
        assertTrue(sql.contains("fraud.events_unique"));
        assertTrue(sql.contains("shopId != 'TEST'"));
        assertTrue(sql.contains("checkedTemplate = :checkedTemplate"));
        assertTrue(sql.contains("checkedRule = :checkedRule"));
        assertEquals("shop-template", FilterUtil.initParams(filter).getValue("checkedTemplate"));
        assertEquals("many_emails_per_card", FilterUtil.initParams(filter).getValue("checkedRule"));
    }

    @Test
    void appliesTemplateAndRuleDirectlyForFraudResults() {
        FilterDto filter = new FilterDto();
        SortDto sort = new SortDto();
        sort.setOrder(SortOrder.DESC);
        filter.setSort(sort);
        filter.setSearchFields(Set.of(
                searchField(PaymentField.CHECKED_TEMPLATE, FieldType.FRAUD_RESULT, "shop-template"),
                searchField(PaymentField.CHECKED_RULE, FieldType.FRAUD_RESULT, "many_emails_per_card")
        ));

        String sql = FilterUtil.appendFraudResultFilters(filter);

        assertTrue(sql.contains("checkedTemplate = :checkedTemplate"));
        assertTrue(sql.contains("checkedRule = :checkedRule"));
        assertFalse(sql.contains("id in (select id"));
    }

    @Test
    void doesNotApplyFraudResultFieldsToOtherHistoryQueries() {
        FilterDto filter = new FilterDto();
        SortDto sort = new SortDto();
        sort.setOrder(SortOrder.DESC);
        filter.setSort(sort);
        filter.setSearchFields(Set.of(
                searchField(PaymentField.CHECKED_TEMPLATE, FieldType.FRAUD_RESULT, "shop-template"),
                searchField(PaymentField.CHECKED_RULE, FieldType.FRAUD_RESULT, "many_emails_per_card")
        ));

        String sql = FilterUtil.appendFilters(filter);

        assertFalse(sql.contains("checkedTemplate"));
        assertFalse(sql.contains("checkedRule"));
        assertFalse(sql.contains("id in (select id"));
    }

    private SearchFieldDto searchField(PaymentField field, FieldType type, String value) {
        return SearchFieldDto.builder()
                .field(field)
                .type(type)
                .value(value)
                .build();
    }
}
