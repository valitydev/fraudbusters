package dev.vality.fraudbusters.repository;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.damsel.fraudbusters.Chargeback;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationStatusGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.ChargebackRepository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.ChargebackMapper;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
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
        TestClickhouseConfig.class,
        ClickhouseProperties.class,
        PaymentTypeByContextResolver.class,
        ChargebackRepository.class,
        ChargebackMapper.class,
        AggregationStatusGeneralRepositoryImpl.class
},
        initializers = HistoricalChargebackDataTest.Initializer.class)
class HistoricalChargebackDataTest {

    @Autowired
    private Repository<Chargeback> chargebackRepository;

    @Test
    void getChargebacksByTimeSlot() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);

        assertFalse(chargebacks.isEmpty());
        assertEquals(6, chargebacks.size());
    }

    @Test
    void getChargebacksByTimeSlotAndSearchPatterns() {
        FilterDto filter = createFilterDto();

        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);

        assertFalse(chargebacks.isEmpty());
        assertEquals(1, chargebacks.size());
        assertEquals("2035728", chargebacks.get(0).getReferenceInfo().getMerchantInfo().getShopId());
        assertEquals("partyId_2", chargebacks.get(0).getReferenceInfo().getMerchantInfo().getPartyId());
    }

    @NotNull
    private FilterDto createFilterDto() {
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
        return filter;
    }

    @Test
    void getChargebacksByTimeSlotAndLimitSize() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);

        assertFalse(chargebacks.isEmpty());
        assertEquals(3, chargebacks.size());
    }

    @Test
    void getChargebacksByTimeSlotAndPageAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setLastId("1DkraVdGJfs.1|rejected");
        SortDto sortDto = new SortDto();
        sortDto.setOrder(SortOrder.DESC);
        filter.setSort(sortDto);

        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);

        assertFalse(chargebacks.isEmpty());
        assertEquals(3, chargebacks.size());
    }

    @Test
    void getChargebacksByTimeSlotAndPageAndSearchPatternsAndSort() {
        FilterDto filter = new FilterDto();
        filter.setTimeFrom("2020-05-01T18:04:53");
        filter.setTimeTo("2020-10-01T18:04:53");
        filter.setSize(3L);
        filter.setLastId("1DkraVdGJfs.1|rejected");
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

        List<Chargeback> chargebacks = chargebackRepository.getByFilter(filter);

        assertFalse(chargebacks.isEmpty());
        assertEquals(2, chargebacks.size());
        assertEquals("partyId_2", chargebacks.get(0).getReferenceInfo().getMerchantInfo().getPartyId());
        assertEquals("partyId_2", chargebacks.get(1).getReferenceInfo().getMerchantInfo().getPartyId());
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                    "sql/data/insert_history_chargebacks.sql"
            ));
        }
    }

}