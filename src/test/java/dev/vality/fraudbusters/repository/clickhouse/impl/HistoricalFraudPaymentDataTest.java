package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.FraudPaymentRowMapper;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@ContextConfiguration(classes = {
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        PaymentTypeByContextResolver.class,
        FraudPaymentRepository.class,
        FraudPaymentRowMapper.class,
        AggregationStatusGeneralRepositoryImpl.class})
class HistoricalFraudPaymentDataTest {

    @Autowired
    private Repository<FraudPaymentRow> fraudPaymentRowRepository;

    @BeforeAll
    static void setUp() throws Exception {
        ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                "sql/data/insert_history_fraud_payments.sql"
        ));
    }

    @Test
    void getFraudPaymentsByTimeSlot() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<FraudPaymentRow> fraudPaymentRows = fraudPaymentRowRepository.getByFilter(filter);

        assertFalse(fraudPaymentRows.isEmpty());
        assertEquals(6, fraudPaymentRows.size());
    }

    @Test
    void getFraudPaymentsByTimeSlotAndSearchPatterns() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        Set<SearchFieldDto> searchFields = new HashSet<>();
        searchFields.add(SearchFieldDto.builder()
                .field(PaymentField.PARTY_ID)
                .type(FieldType.STRING)
                .value("partyId_2")
                .build());
        searchFields.add(SearchFieldDto.builder()
                .field(PaymentField.SHOP_ID)
                .type(FieldType.STRING)
                .value("2035728")
                .build());
        filter.setSearchFields(searchFields);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<FraudPaymentRow> fraudPaymentRows = fraudPaymentRowRepository.getByFilter(filter);

        assertFalse(fraudPaymentRows.isEmpty());
        assertEquals(1, fraudPaymentRows.size());
        assertEquals("2035728", fraudPaymentRows.get(0).getShopId());
        assertEquals("partyId_2", fraudPaymentRows.get(0).getPartyId());
    }

    @Test
    void getFraudPaymentsByTimeSlotAndLimitSize() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<FraudPaymentRow> fraudPaymentRows = fraudPaymentRowRepository.getByFilter(filter);

        assertFalse(fraudPaymentRows.isEmpty());
        assertEquals(3, fraudPaymentRows.size());
    }

    @Test
    void getFraudPaymentsByTimeSlotAndPageAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setLastId("3");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<FraudPaymentRow> fraudPaymentRows = fraudPaymentRowRepository.getByFilter(filter);

        assertFalse(fraudPaymentRows.isEmpty());
        assertEquals(3, fraudPaymentRows.size());
    }

    @Test
    void getFraudPaymentsByTimeSlotAndPageAndSearchPatternsAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        filter.setLastId("2");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);
        Set<SearchFieldDto> searchFields = new HashSet<>();
        searchFields.add(SearchFieldDto.builder()
                .field(PaymentField.PARTY_ID)
                .type(FieldType.STRING)
                .value("group_1")
                .build());
        filter.setSearchFields(searchFields);

        List<FraudPaymentRow> fraudPaymentRows = fraudPaymentRowRepository.getByFilter(filter);

        assertFalse(fraudPaymentRows.isEmpty());
        assertEquals(2, fraudPaymentRows.size());
        assertEquals("group_1", fraudPaymentRows.get(0).getPartyId());
        assertEquals("group_1", fraudPaymentRows.get(1).getPartyId());
    }
}
