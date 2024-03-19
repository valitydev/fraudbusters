package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.CheckedPaymentMapper;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles("full-prod")
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@ContextConfiguration(classes = {
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        CheckedPaymentMapper.class,
        PaymentRepositoryImpl.class})
class HistoricalPaymentDataTest {

    @Autowired
    private Repository<CheckedPayment> paymentRepository;

    @BeforeAll
    static void setUp() throws Exception {
        ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                "sql/data/insert_history_payments.sql"
        ));
    }

    @Test
    void getPaymentsByTimeSlot() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(6, payments.size());
    }

    @Test
    void getPaymentsByTimeSlotAndSearchPatterns() {
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

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(1, payments.size());
        assertEquals("2035728", payments.get(0).getShopId());
        assertEquals("partyId_2", payments.get(0).getPartyId());
    }

    @Test
    void getPaymentsByTimeSlotAndLimitSize() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(3, payments.size());
    }

    @Test
    void getPaymentsByTimeSlotAndPageAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setLastId("3");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(3, payments.size());
    }

    @Test
    void getPaymentsByTimeSlotAndPageAndSearchPatternsAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        filter.setLastId("1");
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

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(1, payments.size());
        assertEquals("partyId_2", payments.get(0).getPartyId());
    }

    @Test
    void getPaymentsByTimeSlotAndSearchByStatusEnum() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);
        Set<SearchFieldDto> searchFields = new HashSet<>();
        String filterStatus = "captured";
        searchFields.add(SearchFieldDto.builder()
                .field(PaymentField.STATUS)
                .type(FieldType.ENUM)
                .value(filterStatus)
                .build());
        filter.setSearchFields(searchFields);

        List<CheckedPayment> payments = paymentRepository.getByFilter(filter);

        assertFalse(payments.isEmpty());
        assertEquals(2, payments.size());
        assertEquals(filterStatus, payments.get(0).getPaymentStatus());
        assertEquals(filterStatus, payments.get(1).getPaymentStatus());
    }

}
