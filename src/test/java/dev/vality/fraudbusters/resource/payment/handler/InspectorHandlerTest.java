package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.fraudbusters.BlockedShops;
import dev.vality.damsel.fraudbusters.ClientInfo;
import dev.vality.damsel.fraudbusters.InspectUserContext;
import dev.vality.damsel.fraudbusters.ShopContext;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
class InspectorHandlerTest {

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
    void inspectUserShopsBlocked() throws TException {
        InspectorHandler fraudInspectorHandler = new InspectorHandler(
                templateVisitor
        );

        when(templateVisitor.visit(any())).thenAnswer(invocation -> {
            PaymentModel model = invocation.getArgument(0);
            if (model != null && "shop_1".equals(model.getShopId())) {
                return createCheckedResult(ResultStatus.DECLINE);
            }
            return createCheckedResult(ResultStatus.THREE_DS);
        });

        BlockedShops blockedShops = fraudInspectorHandler.inspectUserShops(createInspectUserContext());

        assertEquals(1, blockedShops.getShopListSize());
        assertEquals("shop_1", blockedShops.getShopList().get(0).getShopId());

        ArgumentCaptor<PaymentModel> captor = ArgumentCaptor.forClass(PaymentModel.class);
        verify(templateVisitor, times(2)).visit(captor.capture());
        for (PaymentModel model : captor.getAllValues()) {
            assertEquals("party_1", model.getPartyId());
            assertEquals("user@email.com", model.getEmail());
            assertEquals("79990001122", model.getPhone());
        }
    }

    private InspectUserContext createInspectUserContext() {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setEmail("User@Email.Com");
        clientInfo.setPhone("79990001122");
        return new InspectUserContext()
                .setUserInfo(clientInfo)
                .setShopList(List.of(
                        createShopContext("party_1", "shop_1"),
                        createShopContext("party_1", "shop_2")
                ));
    }

    private ShopContext createShopContext(String partyId, String shopId) {
        return new ShopContext()
                .setPartyId(partyId)
                .setShopId(shopId);
    }

    private CheckedResultModel createCheckedResult(ResultStatus status) {
        CheckedResultModel checkedResultModel = new CheckedResultModel();
        checkedResultModel.setResultModel(new ConcreteResultModel(status, null, null));
        return checkedResultModel;
    }
}
