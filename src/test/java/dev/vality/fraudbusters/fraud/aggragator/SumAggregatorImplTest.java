package dev.vality.fraudbusters.fraud.aggragator;

import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.aggregator.clickhouse.SumAggregatorImpl;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import dev.vality.fraudbusters.repository.AggregationRepository;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.service.TimeBoundaryService;
import dev.vality.fraudbusters.service.TimeBoundaryServiceImpl;
import dev.vality.fraudo.model.TimeWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dev.vality.fraudo.constant.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SumAggregatorImplTest {

    private SumAggregatorImpl sumAggregator;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AggregationRepository analyticsRefundRepository;
    @Mock
    private AggregationRepository analyticsChargebackRepository;
    @Mock
    private DatabasePaymentFieldResolver databasePaymentFieldResolver;

    private final TimeBoundaryService timeBoundaryService = new TimeBoundaryServiceImpl();

    private final FieldModel modelMock = new FieldModel("name", "value");

    @BeforeEach
    public void init() {
        when(databasePaymentFieldResolver.resolve(any(), any())).thenReturn(modelMock);

        sumAggregator = new SumAggregatorImpl(
                databasePaymentFieldResolver,
                paymentRepository,
                analyticsRefundRepository,
                analyticsChargebackRepository,
                timeBoundaryService
        );
    }

    @Test
    public void sum() {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setAmount(1L);

        when(paymentRepository.sumOperationByFieldWithGroupBy(any(), any(), any(), any(), any())).thenReturn(1050100L);

        Double some = sumAggregator.sum(
                PaymentCheckedField.BIN,
                paymentModel,
                TimeWindow.builder().start(1444).timeUnit(MINUTES).build(),
                null
        );

        assertEquals(Double.valueOf(1050101), some);
    }

    @Test
    public void sumTimeWindow() {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setAmount(1L);
        TimeWindow.TimeWindowBuilder timeWindowBuilder = TimeWindow.builder()
                .start(1444)
                .timeUnit(MINUTES)
                .end(400);
        when(paymentRepository.sumOperationByFieldWithGroupBy(any(), any(), any(), any(), any())).thenReturn(1050100L);

        Double sum = sumAggregator.sum(PaymentCheckedField.BIN, paymentModel, timeWindowBuilder.build(), null);

        assertEquals(Double.valueOf(1050101), sum);

        timeWindowBuilder = TimeWindow.builder()
                .start(1444)
                .timeUnit(MINUTES);
        sum = sumAggregator.sum(PaymentCheckedField.BIN, paymentModel, timeWindowBuilder.build(), null);

        assertEquals(Double.valueOf(1050101), sum);
    }

    @Test
    public void sumSuccess() {
        when(paymentRepository.sumOperationSuccessWithGroupBy(any(), any(), any(), any(), any())).thenReturn(1050100L);
        Double some = sumAggregator.sumSuccess(PaymentCheckedField.BIN, new PaymentModel(),
                TimeWindow.builder()
                        .start(1444)
                        .timeUnit(MINUTES)
                        .build(),
                null
        );

        assertEquals(Double.valueOf(1050100), some);
    }

    @Test
    public void sumError() {
        when(paymentRepository.sumOperationErrorWithGroupBy(any(), any(), any(), any(), any(), any()))
                .thenReturn(1050100L);
        Double some = sumAggregator.sumError(PaymentCheckedField.BIN, new PaymentModel(),
                TimeWindow.builder()
                        .start(1444)
                        .timeUnit(MINUTES)
                        .build(),
                null,
                null
        );

        assertEquals(Double.valueOf(1050100), some);
    }

    @Test
    public void sumErrorWithoutCode() {
        when(paymentRepository.sumOperationErrorWithGroupBy(any(), any(), any(), any(), any()))
                .thenReturn(1050100L);
        Double some = sumAggregator.sumError(PaymentCheckedField.BIN, new PaymentModel(),
                TimeWindow.builder()
                        .start(1444)
                        .timeUnit(MINUTES)
                        .build(),
                null
        );

        assertEquals(Double.valueOf(1050100), some);
    }
}
