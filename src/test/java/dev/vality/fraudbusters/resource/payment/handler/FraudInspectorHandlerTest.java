package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.domain.Category;
import dev.vality.damsel.domain.ContactInfo;
import dev.vality.damsel.domain.PartyConfigRef;
import dev.vality.damsel.domain.ShopConfigRef;
import dev.vality.damsel.proxy_inspector.BlackListContext;
import dev.vality.damsel.proxy_inspector.BlockedShops;
import dev.vality.damsel.proxy_inspector.InspectUserContext;
import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.damsel.proxy_inspector.Party;
import dev.vality.damsel.proxy_inspector.Shop;
import dev.vality.damsel.proxy_inspector.ShopContext;
import dev.vality.damsel.domain.ShopLocation;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @BeforeEach
    void setUp() {
        Mockito.reset(
                checkedResultToRiskScoreConverter,
                requestConverter,
                templateVisitor,
                kafkaFraudResultTemplate,
                wbListServiceSrv
        );
    }

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

    @Test
    void inspectUserShopsBlocked() throws TException {
        FraudInspectorHandler fraudInspectorHandler = new FraudInspectorHandler(
                "test",
                checkedResultToRiskScoreConverter,
                requestConverter,
                templateVisitor,
                kafkaFraudResultTemplate,
                wbListServiceSrv
        );

        when(templateVisitor.visit(any())).thenAnswer(invocation -> {
            PaymentModel model = invocation.getArgument(0);
            if (model != null && "shop_1".equals(model.getShopId())) {
                return createCheckedResult(ResultStatus.DECLINE);
            }
            return createCheckedResult(ResultStatus.THREE_DS);
        });

        BlockedShops blockedShops = fraudInspectorHandler.inspectUser(createInspectUserContext());

        assertEquals(1, blockedShops.getShopListSize());
        assertEquals("shop_1", blockedShops.getShopList().get(0).getShop().getShopRef().getId());

        ArgumentCaptor<PaymentModel> captor = ArgumentCaptor.forClass(PaymentModel.class);
        verify(templateVisitor, times(2)).visit(captor.capture());
        for (PaymentModel model : captor.getAllValues()) {
            assertEquals("party_1", model.getPartyId());
            assertEquals("user@email.com", model.getEmail());
            assertEquals("79990001122", model.getPhone());
        }
    }

    private static BlackListContext createBlackListContext() {
        return new BlackListContext()
                .setValue("test")
                .setFieldName("field_test")
                .setFirstId("test_id")
                .setSecondId("test_sec_id");
    }

    private static InspectUserContext createInspectUserContext() {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail("User@Email.Com");
        contactInfo.setPhoneNumber("79990001122");
        return new InspectUserContext()
                .setUserInfo(contactInfo)
                .setShopList(List.of(
                        createShopContext("party_1", "shop_1"),
                        createShopContext("party_1", "shop_2")
                ));
    }

    private static ShopContext createShopContext(String partyId, String shopId) {
        ShopLocation location = new ShopLocation();
        location.setUrl("http://example.com");
        return new ShopContext()
                .setParty(new Party(new PartyConfigRef(partyId)))
                .setShop(new Shop(
                        new ShopConfigRef(shopId),
                        new Category("category", "category"),
                        "shop-name",
                        location
                ));
    }

    private static CheckedResultModel createCheckedResult(ResultStatus status) {
        CheckedResultModel checkedResultModel = new CheckedResultModel();
        checkedResultModel.setResultModel(new ConcreteResultModel(status, null, null));
        return checkedResultModel;
    }
}
