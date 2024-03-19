package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.RefundMapper;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@ContextConfiguration(classes = {
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        PaymentTypeByContextResolver.class,
        RefundRepository.class,
        RefundMapper.class,
        AggregationStatusGeneralRepositoryImpl.class})
class HistoricalRefundDataTest {

    @Autowired
    private Repository<Refund> refundRepository;

    @BeforeAll
    static void setUp() throws Exception {
        ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                "sql/data/insert_history_refunds.sql"
        ));
    }

    @Test
    void getRefundsByTimeSlot() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Refund> refunds = refundRepository.getByFilter(filter);

        assertFalse(refunds.isEmpty());
        assertEquals(6, refunds.size());
    }

    @Test
    void getRefundsByTimeSlotAndSearchPatterns() {
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

        List<Refund> refunds = refundRepository.getByFilter(filter);

        assertFalse(refunds.isEmpty());
        assertEquals(1, refunds.size());
        assertEquals("2035728", refunds.get(0).getReferenceInfo().getMerchantInfo().getShopId());
        assertEquals("partyId_2", refunds.get(0).getReferenceInfo().getMerchantInfo().getPartyId());
    }

    @Test
    void getRefundsByTimeSlotAndLimitSize() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Refund> refunds = refundRepository.getByFilter(filter);

        assertFalse(refunds.isEmpty());
        assertEquals(3, refunds.size());
    }

    @Test
    void getRefundsByTimeSlotAndPageAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setLastId("3");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Refund> refunds = refundRepository.getByFilter(filter);

        assertFalse(refunds.isEmpty());
        assertEquals(3, refunds.size());
    }

    @Test
    void getRefundsByTimeSlotAndPageAndSearchPatternsAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);
        Set<SearchFieldDto> searchFields = new HashSet<>();
        searchFields.add(SearchFieldDto.builder()
                .field(PaymentField.PARTY_ID)
                .type(FieldType.STRING)
                .value("partyId_2")
                .build());
        filter.setSearchFields(searchFields);

        List<Refund> refunds = refundRepository.getByFilter(filter);

        assertFalse(refunds.isEmpty());
        assertEquals(2, refunds.size());
        assertEquals("partyId_2", refunds.get(0).getReferenceInfo().getMerchantInfo().getPartyId());
        assertEquals("partyId_2", refunds.get(1).getReferenceInfo().getMerchantInfo().getPartyId());
    }
}