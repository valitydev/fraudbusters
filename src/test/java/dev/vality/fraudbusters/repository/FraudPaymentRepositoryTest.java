package dev.vality.fraudbusters.repository;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.domain.TimeProperties;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationStatusGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.FraudPaymentRepository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.FraudPaymentRowMapper;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        DatabasePaymentFieldResolver.class,
        AggregationGeneralRepositoryImpl.class,
        AggregationStatusGeneralRepositoryImpl.class,
        FraudPaymentRepository.class,
        FraudPaymentRowMapper.class
})
public class FraudPaymentRepositoryTest {

    private static final String SELECT_COUNT_AS_CNT_FROM_FRAUD_EVENTS_UNIQUE =
            "SELECT count() as cnt from fraud.fraud_payment";

    @Autowired
    DatabasePaymentFieldResolver databasePaymentFieldResolver;
    @MockBean
    ColumbusServiceSrv.Iface iface;
    @Autowired
    private FraudPaymentRepository fraudPaymentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @NotNull
    public static FraudPaymentRow createFraudPaymentRow(String id) {
        FraudPaymentRow fraudPaymentRow = new FraudPaymentRow();
        fraudPaymentRow.setId(id);
        TimeProperties timeProperties = TimestampUtil.generateTimeProperties();
        fraudPaymentRow.setTimestamp(timeProperties.getTimestamp());
        fraudPaymentRow.setEventTimeHour(timeProperties.getEventTimeHour());
        fraudPaymentRow.setEventTime(timeProperties.getEventTime());
        fraudPaymentRow.setComment("");
        fraudPaymentRow.setType("Card not present");
        fraudPaymentRow.setPaymentStatus(PaymentStatus.captured.name());
        return fraudPaymentRow;
    }

    public static FraudPayment createFraudPayment(String id) {
        return new FraudPayment()
                .setId(id)
                .setEventTime(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern(TimestampUtil.YYYY_MM_DD_HH_MM_SS)))
                .setComment("")
                .setType("Card not present");
    }

    @Test
    public void insertBatch() throws SQLException {
        fraudPaymentRepository.insertBatch(createBatch());

        Integer count = jdbcTemplate.queryForObject(
                SELECT_COUNT_AS_CNT_FROM_FRAUD_EVENTS_UNIQUE,
                (resultSet, i) -> resultSet.getInt("cnt")
        );

        assertEquals(2, count.intValue());
    }

    @NotNull
    private List<FraudPaymentRow> createBatch() {
        FraudPaymentRow value = createFraudPaymentRow("inv1.1");
        FraudPaymentRow value2 = createFraudPaymentRow("inv2.1");
        return List.of(value, value2);
    }

}
