package dev.vality.fraudbusters.repository.clickhouse.impl;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.fraudbusters.config.ClickhouseConfig;
import dev.vality.fraudbusters.constant.PaymentField;
import dev.vality.fraudbusters.constant.SortOrder;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.RefundMapper;
import dev.vality.fraudbusters.service.dto.FieldType;
import dev.vality.fraudbusters.service.dto.FilterDto;
import dev.vality.fraudbusters.service.dto.SearchFieldDto;
import dev.vality.fraudbusters.service.dto.SortDto;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ClickhouseConfig.class, PaymentTypeByContextResolver.class,
        RefundRepository.class, RefundMapper.class, AggregationStatusGeneralRepositoryImpl.class},
        initializers = HistoricalRefundDataTest.Initializer.class)
class HistoricalRefundDataTest {

    @Autowired
    private Repository<Refund> refundRepository;

    @Container
    public static ClickHouseContainer clickHouseContainer =
            new ClickHouseContainer("yandex/clickhouse-server:19.17");


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
        filter.setLastId("1DkraVdGJfs.1|failed");
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
        filter.setLastId("1DkraVdGJfs.1|failed");
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


    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues
                    .of(
                            "clickhouse.db.url=" + clickHouseContainer.getJdbcUrl(),
                            "clickhouse.db.user=" + clickHouseContainer.getUsername(),
                            "clickhouse.db.password=" + clickHouseContainer.getPassword()
                    )
                    .applyTo(configurableApplicationContext.getEnvironment());
            ChInitializer.initAllScripts(clickHouseContainer, List.of(
                    "sql/db_init.sql",
                    "sql/V4__create_payment.sql",
                    "sql/V5__add_fields.sql",
                    "sql/V6__add_result_fields_payment.sql",
                    "sql/V7__add_fields.sql",
                    "sql/data/insert_history_refunds.sql"
            ));
        }
    }
}