package dev.vality.fraudbusters.repository;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.EventField;
import dev.vality.fraudbusters.converter.FraudResultToEventConverter;
import dev.vality.fraudbusters.domain.*;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.FraudResultRepository;
import dev.vality.fraudbusters.repository.clickhouse.mapper.EventMapper;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        FraudResultToEventConverter.class,
        ClickhouseProperties.class,
        TestClickhouseConfig.class,
        DatabasePaymentFieldResolver.class,
        AggregationGeneralRepositoryImpl.class,
        FraudResultRepository.class,
        EventMapper.class
})
public class FraudResultRepositoryTest {

    private static final String SELECT_COUNT_AS_CNT_FROM_FRAUD_EVENTS_UNIQUE =
            "SELECT count() as cnt from fraud.events_unique";

    @Autowired
    FraudResultToEventConverter fraudResultToEventConverter;
    @Autowired
    DatabasePaymentFieldResolver databasePaymentFieldResolver;
    @MockBean
    ColumbusServiceSrv.Iface iface;
    @Autowired
    private FraudResultRepository fraudResultRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        ChInitializer.initAllScripts(ClickHouseContainerExtension.CLICKHOUSE_CONTAINER, List.of(
                "sql/db_init.sql",
                "sql/V3__create_fraud_payments.sql",
                "sql/V4__create_payment.sql",
                "sql/V5__add_fields.sql",
                "sql/V6__add_result_fields_payment.sql",
                "sql/V7__add_fields.sql",
                "sql/V8__create_withdrawal.sql",
                "sql/V10__add_id_inspect_result.sql",
                "sql/V11__rename_masked_pan.sql"
        ));
    }

    @Test
    public void insertBatch() throws SQLException {
        fraudResultRepository.insertBatch(
                createBatch().stream()
                        .map(fraudResultToEventConverter::convert)
                        .collect(Collectors.toList())
        );

        Integer count = jdbcTemplate.queryForObject(
                SELECT_COUNT_AS_CNT_FROM_FRAUD_EVENTS_UNIQUE,
                (resultSet, i) -> resultSet.getInt("cnt")
        );

        assertEquals(2, count.intValue());
    }

    @NotNull
    private List<FraudResult> createBatch() {
        FraudResult value = createFraudResult(ResultStatus.ACCEPT, BeanUtil.createPaymentModel());
        FraudResult value2 = createFraudResult(ResultStatus.DECLINE, BeanUtil.createFraudModelSecond());
        return List.of(value, value2);
    }

    @NotNull
    private FraudResult createFraudResult(ResultStatus decline, PaymentModel paymentModel) {
        FraudResult value2 = new FraudResult();
        CheckedResultModel resultModel = new CheckedResultModel();
        resultModel.setResultModel(new ConcreteResultModel(decline, "test", null));
        resultModel.setCheckedTemplate("RULE");

        value2.setResultModel(resultModel);
        FraudRequest fraudRequest = new FraudRequest();
        fraudRequest.setFraudModel(paymentModel);
        Metadata metadata = new Metadata();
        fraudRequest.setMetadata(metadata);
        value2.setFraudRequest(fraudRequest);
        return value2;
    }

    @Test
    public void countOperationByEmailTest() throws SQLException {
        Instant now = Instant.now();
        Long to = now.toEpochMilli();
        Long from = now.minus(10L, ChronoUnit.MINUTES).toEpochMilli();
        List<FraudResult> batch = createBatch();
        fraudResultRepository.insertBatch(fraudResultToEventConverter.convertBatch(batch));

        int count = fraudResultRepository.countOperationByField(EventField.email.name(), BeanUtil.EMAIL, from, to);
        assertEquals(1, count);
    }

    @Test
    public void countOperationByEmailTestWithGroupBy() throws SQLException {
        PaymentModel paymentModel = BeanUtil.createFraudModelSecond();
        paymentModel.setPartyId("test");
        fraudResultRepository.insertBatch(fraudResultToEventConverter
                .convertBatch(List.of(
                                createFraudResult(ResultStatus.ACCEPT, BeanUtil.createPaymentModel()),
                                createFraudResult(ResultStatus.DECLINE, BeanUtil.createFraudModelSecond()),
                                createFraudResult(ResultStatus.ACCEPT, paymentModel),
                                createFraudResult(ResultStatus.DECLINE, paymentModel)
                        )
                )
        );

        Instant now = Instant.now().plusSeconds(30L);
        Long to = now.toEpochMilli();
        Long from = now.minus(10L, ChronoUnit.MINUTES).toEpochMilli();

        FieldModel email = databasePaymentFieldResolver.resolve(PaymentCheckedField.EMAIL, paymentModel);
        int count = fraudResultRepository.countOperationByFieldWithGroupBy(EventField.email.name(),
                email.getValue(),
                from,
                to,
                List.of()
        );
        assertEquals(3, count);

        count = fraudResultRepository.countOperationSuccessWithGroupBy(EventField.email.name(),
                email.getValue(),
                from,
                to,
                List.of()
        );
        assertEquals(1, count);

        FieldModel resolve = databasePaymentFieldResolver.resolve(PaymentCheckedField.PARTY_ID, paymentModel);
        count = fraudResultRepository.countOperationByFieldWithGroupBy(EventField.email.name(),
                email.getValue(),
                from,
                to,
                List.of(resolve)
        );
        assertEquals(2, count);

        count = fraudResultRepository.countOperationSuccessWithGroupBy(EventField.email.name(),
                email.getValue(),
                from,
                to,
                List.of(resolve)
        );
        assertEquals(1, count);
    }

    @Test
    public void sumOperationByEmailTest() throws SQLException {
        fraudResultRepository.insertBatch(fraudResultToEventConverter.convertBatch(createBatch()));

        Instant now = Instant.now();
        Long to = now.toEpochMilli();
        Long from = now.minus(10L, ChronoUnit.MINUTES).toEpochMilli();
        Long sum = fraudResultRepository.sumOperationByFieldWithGroupBy(EventField.email.name(),
                BeanUtil.EMAIL,
                from,
                to,
                List.of()
        );
        assertEquals(BeanUtil.AMOUNT_FIRST, sum);

        sum = fraudResultRepository.sumOperationSuccessWithGroupBy(EventField.email.name(),
                BeanUtil.EMAIL,
                from,
                to,
                List.of()
        );
        assertEquals(BeanUtil.AMOUNT_FIRST, sum);
    }

    @Test
    public void countUniqOperationTest() {
        PaymentModel paymentModel = BeanUtil.createPaymentModel();
        paymentModel.setFingerprint("test");
        fraudResultRepository.insertBatch(fraudResultToEventConverter
                .convertBatch(List.of(
                                createFraudResult(ResultStatus.ACCEPT, BeanUtil.createPaymentModel()),
                                createFraudResult(ResultStatus.DECLINE, BeanUtil.createFraudModelSecond()),
                                createFraudResult(ResultStatus.DECLINE, BeanUtil.createPaymentModel()),
                                createFraudResult(ResultStatus.DECLINE, paymentModel)
                        )
                )
        );

        Instant now = Instant.now();
        Long to = now.toEpochMilli();
        Long from = now.minus(10L, ChronoUnit.MINUTES).toEpochMilli();
        Integer sum = fraudResultRepository.uniqCountOperation(EventField.email.name(),
                BeanUtil.EMAIL,
                EventField.fingerprint.name(),
                from,
                to
        );
        assertEquals(Integer.valueOf(2), sum);
    }

    @Test
    public void countUniqOperationWithGroupByTest() {
        PaymentModel paymentModel = BeanUtil.createPaymentModel();
        paymentModel.setFingerprint("test");
        paymentModel.setPartyId("party");

        fraudResultRepository.insertBatch(fraudResultToEventConverter
                .convertBatch(List.of(
                                createFraudResult(ResultStatus.ACCEPT, BeanUtil.createPaymentModel()),
                                createFraudResult(ResultStatus.DECLINE, BeanUtil.createFraudModelSecond()),
                                createFraudResult(ResultStatus.DECLINE, BeanUtil.createPaymentModel()),
                                createFraudResult(ResultStatus.DECLINE, paymentModel)
                        )
                )
        );

        Instant now = Instant.now();
        Long to = now.toEpochMilli();
        Long from = now.minus(10L, ChronoUnit.MINUTES).toEpochMilli();
        Integer sum = fraudResultRepository.uniqCountOperationWithGroupBy(EventField.email.name(),
                BeanUtil.EMAIL,
                EventField.fingerprint.name(),
                from,
                to,
                List.of()
        );
        assertEquals(Integer.valueOf(2), sum);

        FieldModel resolve = databasePaymentFieldResolver.resolve(PaymentCheckedField.PARTY_ID, paymentModel);
        sum = fraudResultRepository.uniqCountOperationWithGroupBy(EventField.email.name(),
                BeanUtil.EMAIL,
                EventField.fingerprint.name(),
                from,
                to,
                List.of(resolve)
        );
        assertEquals(Integer.valueOf(1), sum);
    }

}
