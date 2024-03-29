package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.domain.Event;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.EventMapper;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@ContextConfiguration(classes = {
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        PaymentTypeByContextResolver.class,
        FraudResultRepository.class,
        EventMapper.class,
        AggregationGeneralRepositoryImpl.class},
        initializers = HistoricalFraudResultDataTest.Initializer.class)
class HistoricalFraudResultDataTest {

    @Autowired
    private Repository<Event> fraudResultRepository;

    @BeforeAll
    static void setUp() throws Exception {
        ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                "sql/data/insert_history_fraud_results.sql"
        ));
    }

    @Test
    void getFraudResultsByTimeSlot() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);

        assertFalse(fraudResults.isEmpty());
        assertTrue(fraudResults.size() >= 6);
    }

    @Test
    void getFraudResultsByTimeSlotAndSearchPatterns() {
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

        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);

        assertFalse(fraudResults.isEmpty());
        assertEquals(1, fraudResults.size());
        assertEquals("2035728", fraudResults.get(0).getShopId());
        assertEquals("partyId_2", fraudResults.get(0).getPartyId());
    }

    @Test
    void getFraudResultsByTimeSlotAndLimitSize() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);

        assertFalse(fraudResults.isEmpty());
        assertEquals(3, fraudResults.size());
    }

    @Test
    void getFraudResultsByTimeSlotAndPageAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setLastId("3");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);

        assertFalse(fraudResults.isEmpty());
        assertEquals(3, fraudResults.size());
    }

    @Test
    void getFraudResultsByTimeSlotAndPageAndSearchPatternsAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(5L);
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

        List<Event> fraudResults = fraudResultRepository.getByFilter(filter);

        assertFalse(fraudResults.isEmpty());
        assertEquals(2, fraudResults.size());
        assertEquals("partyId_2", fraudResults.get(0).getPartyId());
        assertEquals("partyId_2", fraudResults.get(1).getPartyId());
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                    "sql/data/insert_history_fraud_results.sql"
            ));
        }
    }
}