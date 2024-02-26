package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.proxy_inspector.BlackListContext;
import dev.vality.damsel.wb_list.ListNotFound;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.converter.CheckedResultToRiskScoreConverter;
import dev.vality.fraudbusters.converter.ContextToFraudRequestConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class FraudInspectorHandlerTest {

    @MockBean
    CheckedResultToRiskScoreConverter checkedResultToRiskScoreConverter;
    @MockBean
    ContextToFraudRequestConverter requestConverter;
    @MockBean
    TemplateVisitor<PaymentModel, CheckedResultModel> templateVisitor;
    @MockBean
    KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate;
    @MockBean
    WbListServiceSrv.Iface wbListServiceSrv;

    @Test
    void isExistInBlackList() throws TException {
        FraudInspectorHandler fraudInspectorHandler = new FraudInspectorHandler(
                "test",
                checkedResultToRiskScoreConverter,
                requestConverter,
                templateVisitor,
                kafkaFraudResultTemplate,
                wbListServiceSrv
        );

        when(wbListServiceSrv.isExist(any())).thenReturn(false);
        boolean existInBlackList = fraudInspectorHandler.isExistInBlackList(createBlackListContext());
        assertEquals(false, existInBlackList);

        when(wbListServiceSrv.isExist(any())).thenReturn(true);
        existInBlackList = fraudInspectorHandler.isExistInBlackList(createBlackListContext());
        assertEquals(true, existInBlackList);

        when(wbListServiceSrv.isExist(any())).thenThrow(new ListNotFound());
        existInBlackList = fraudInspectorHandler.isExistInBlackList(createBlackListContext());
        assertEquals(false, existInBlackList);
    }

    private static BlackListContext createBlackListContext() {
        return new BlackListContext()
                .setValue("test")
                .setFieldName("field_test")
                .setFirstId("test_id")
                .setSecondId("test_sec_id");
    }
}