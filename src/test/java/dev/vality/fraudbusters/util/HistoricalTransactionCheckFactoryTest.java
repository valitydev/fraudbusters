package dev.vality.fraudbusters.util;

import com.rbkmoney.fraudo.constant.ResultStatus;
import dev.vality.damsel.fraudbusters.CheckResult;
import dev.vality.damsel.fraudbusters.ConcreteCheckResult;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.converter.CheckedResultModelToCheckResultConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.factory.TestObjectsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(
        MockitoExtension.class
)
class HistoricalTransactionCheckFactoryTest {

    private HistoricalTransactionCheckFactory factory;

    @Mock
    private CheckedResultModelToCheckResultConverter checkResultConverter;

    private static final String TEMPLATE = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        factory = new HistoricalTransactionCheckFactory(checkResultConverter);
    }

    @Test
    void createHistoricalTransactionCheck() {
        Payment payment = BeanUtil.createPayment(PaymentStatus.captured);
        CheckedResultModel resultModel = TestObjectsFactory.createCheckedResultModel(TEMPLATE, ResultStatus.ACCEPT);
        CheckResult checkResult = new CheckResult()
                .setCheckedTemplate(TEMPLATE)
                .setConcreteCheckResult(new ConcreteCheckResult());
        when(checkResultConverter.convert(resultModel)).thenReturn(checkResult);

        var actual = factory.createHistoricalTransactionCheck(payment, resultModel);

        verify(checkResultConverter, times(1)).convert(resultModel);
        assertEquals(actual.getCheckResult(), checkResult);
        assertEquals(actual.getTransaction(), payment);
    }

}
