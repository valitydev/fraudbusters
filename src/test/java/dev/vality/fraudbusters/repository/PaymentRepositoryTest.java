package dev.vality.fraudbusters.repository;

import dev.vality.clickhouse.initializer.ChInitializer;
import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.fraudbusters.config.TestClickhouseConfig;
import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.constant.EventField;
import dev.vality.fraudbusters.converter.FraudResultToEventConverter;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.PaymentRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.mapper.CheckedPaymentMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static dev.vality.fraudbusters.extension.ClickHouseContainerExtension.CLICKHOUSE_CONTAINER;
import static dev.vality.fraudbusters.util.BeanUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("full-prod")
@Testcontainers
@ExtendWith({SpringExtension.class, ClickHouseContainerExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        TestClickhouseConfig.class,
        ClickhouseProperties.class,
        PaymentRepositoryImpl.class,
        FraudResultToEventConverter.class,
        DatabasePaymentFieldResolver.class,
        AggregationGeneralRepositoryImpl.class,
        CheckedPaymentMapper.class
},
        initializers = PaymentRepositoryTest.Initializer.class)
public class PaymentRepositoryTest {

    public static final long FROM = 1588761200000L;
    public static final long TO = 1588761209000L;

    @Autowired
    DatabasePaymentFieldResolver databasePaymentFieldResolver;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockBean
    ColumbusServiceSrv.Iface iface;
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    public void countOperationByEmailTest() throws SQLException {
        int count = paymentRepository.countOperationByField(EventField.email.name(), EMAIL, FROM, TO);
        assertEquals(1, count);
    }

    @Test
    public void countOperationByPhoneTest() throws SQLException {
        int count = paymentRepository.countOperationByField(EventField.phone.name(), PHONE, FROM, TO);
        assertEquals(1, count);
    }

    @Test
    public void isExistTest() throws SQLException {
        Boolean isExist = paymentRepository.isExistByField(EventField.phone.name(), PHONE, FROM, TO);
        assertTrue(isExist);
    }

    @Test
    public void countOperationByEmailTestWithGroupBy() throws SQLException {
        PaymentModel paymentModel = createFraudModelSecond();

        FieldModel email = databasePaymentFieldResolver.resolve(PaymentCheckedField.EMAIL, paymentModel);
        int count = paymentRepository.countOperationByFieldWithGroupBy(EventField.email.name(), email.getValue(),
                1588761200000L, 1588761209000L, List.of()
        );
        assertEquals(2, count);

        FieldModel resolve = databasePaymentFieldResolver.resolve(PaymentCheckedField.PARTY_ID, paymentModel);
        count = paymentRepository.countOperationByFieldWithGroupBy(EventField.email.name(), email.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve)
        );
        assertEquals(1, count);

        count = paymentRepository.countOperationSuccessWithGroupBy(EventField.email.name(), email.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve)
        );
        assertEquals(1, count);

        count = paymentRepository.countOperationErrorWithGroupBy(EventField.email.name(), email.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve), ""
        );
        assertEquals(0, count);
    }

    @Test
    public void countOperationByPhoneTestWithGroupBy() throws SQLException {
        PaymentModel paymentModel = createFraudModelSecond();

        FieldModel fieldModel = databasePaymentFieldResolver.resolve(PaymentCheckedField.PHONE, paymentModel);
        int count = paymentRepository.countOperationByFieldWithGroupBy(EventField.phone.name(), fieldModel.getValue(),
                1588761200000L, 1588761209000L, List.of()
        );

        assertEquals(2, count);

        FieldModel resolve = databasePaymentFieldResolver.resolve(PaymentCheckedField.PARTY_ID, paymentModel);
        count = paymentRepository.countOperationByFieldWithGroupBy(EventField.phone.name(), fieldModel.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve)
        );
        assertEquals(1, count);

        count = paymentRepository.countOperationSuccessWithGroupBy(EventField.phone.name(), fieldModel.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve)
        );
        assertEquals(1, count);

        count = paymentRepository.countOperationErrorWithGroupBy(EventField.phone.name(), fieldModel.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve), ""
        );
        assertEquals(0, count);

        count = paymentRepository.countOperationErrorWithGroupBy(EventField.phone.name(), fieldModel.getValue(),
                1588761200000L, 1588761209000L, List.of(resolve)
        );
        assertEquals(0, count);
    }

    @Test
    public void sumOperationByEmailTest() throws SQLException {
        Long sum =
                paymentRepository.sumOperationByFieldWithGroupBy(EventField.email.name(), EMAIL, FROM, TO, List.of());
        assertEquals(AMOUNT_FIRST, sum);

        sum = paymentRepository.sumOperationSuccessWithGroupBy(EventField.email.name(), EMAIL, FROM, TO, List.of());
        assertEquals(AMOUNT_FIRST, sum);

        sum = paymentRepository.sumOperationErrorWithGroupBy(EventField.email.name(), EMAIL, FROM, TO, List.of(), "");
        assertEquals(0L, sum.longValue());

        sum = paymentRepository.sumOperationErrorWithGroupBy(EventField.email.name(), EMAIL, FROM, TO, List.of());
        assertEquals(0L, sum.longValue());
    }

    @Test
    public void sumOperationByPhoneTest() throws SQLException {
        Long sum =
                paymentRepository.sumOperationByFieldWithGroupBy(EventField.phone.name(), PHONE, FROM, TO, List.of());
        assertEquals(AMOUNT_FIRST, sum);

        sum = paymentRepository.sumOperationSuccessWithGroupBy(EventField.phone.name(), PHONE, FROM, TO, List.of());
        assertEquals(AMOUNT_FIRST, sum);

        sum = paymentRepository.sumOperationErrorWithGroupBy(EventField.phone.name(), PHONE, FROM, TO, List.of(), "");
        assertEquals(0L, sum.longValue());

        sum = paymentRepository.sumOperationErrorWithGroupBy(EventField.phone.name(), PHONE, FROM, TO, List.of());
        assertEquals(0L, sum.longValue());
    }

    @Test
    public void countUniqOperationTest() {
        Integer sum = paymentRepository.uniqCountOperation(EventField.email.name(), EMAIL + SUFIX,
                EventField.fingerprint.name(), FROM, TO
        );
        assertEquals(Integer.valueOf(2), sum);
    }

    @Test
    public void countUniqOperationWithGroupByTest() {
        PaymentModel paymentModel = createFraudModelSecond();
        Integer sum = paymentRepository.uniqCountOperationWithGroupBy(EventField.email.name(), EMAIL + SUFIX,
                EventField.fingerprint.name(), FROM, TO, List.of()
        );
        assertEquals(Integer.valueOf(2), sum);

        FieldModel resolve = databasePaymentFieldResolver.resolve(PaymentCheckedField.PARTY_ID, paymentModel);
        sum = paymentRepository.uniqCountOperationWithGroupBy(EventField.email.name(), EMAIL + SUFIX,
                EventField.fingerprint.name(), FROM, TO, List.of(resolve)
        );
        assertEquals(Integer.valueOf(1), sum);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            ChInitializer.initAllScripts(CLICKHOUSE_CONTAINER, List.of(
                    "sql/data/inserts_event_sink.sql"
            ));
        }
    }

}

