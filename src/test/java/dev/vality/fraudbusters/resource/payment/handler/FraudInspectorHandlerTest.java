package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.proxy_inspector.BlackListContext;
import dev.vality.damsel.wb_list.ListNotFound;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.converter.CheckedResultToRiskScoreConverter;
import dev.vality.fraudbusters.converter.ContextToFraudRequestConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.ConcreteResultModel;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import dev.vality.fraudo.constant.ResultStatus;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class FraudInspectorHandlerTest {

    @MockitoBean
    CheckedResultToRiskScoreConverter checkedResultToRiskScoreConverter;
    @MockitoBean
    ContextToFraudRequestConverter requestConverter;
    @MockitoBean
    TemplateVisitor<PaymentModel, CheckedResultModel> templateVisitor;
    @MockitoBean
    KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate;
    @MockitoBean
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
        boolean existInBlackList = fraudInspectorHandler.isBlacklisted(createBlackListContext());
        assertEquals(false, existInBlackList);

        when(wbListServiceSrv.isExist(any())).thenReturn(true);
        existInBlackList = fraudInspectorHandler.isBlacklisted(createBlackListContext());
        assertEquals(true, existInBlackList);

        when(wbListServiceSrv.isExist(any())).thenThrow(new ListNotFound());
        existInBlackList = fraudInspectorHandler.isBlacklisted(createBlackListContext());
        assertEquals(false, existInBlackList);
    }

    private BlackListContext createBlackListContext() {
        return new BlackListContext()
                .setValue("test")
                .setFieldName("field_test")
                .setFirstId("test_id")
                .setSecondId("test_sec_id");
    }

    private CheckedResultModel createCheckedResult(ResultStatus status) {
        CheckedResultModel checkedResultModel = new CheckedResultModel();
        checkedResultModel.setResultModel(new ConcreteResultModel(status, null, null));
        return checkedResultModel;
    }
}
