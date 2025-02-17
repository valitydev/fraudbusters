package dev.vality.fraudbusters.service;

import dev.vality.damsel.fraudbusters.MerchantInfo;
import dev.vality.damsel.fraudbusters.ReferenceInfo;
import dev.vality.fraudbusters.config.RestTemplateConfig;
import dev.vality.fraudbusters.config.payment.PaymentPoolConfig;
import dev.vality.fraudbusters.config.properties.DefaultTemplateProperties;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.FraudResultRepository;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudo.payment.visitor.impl.FirstFindVisitorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {ShopManagementService.class,
        AggregationGeneralRepositoryImpl.class,
        FraudResultRepository.class,
        RestTemplateConfig.class,
        InitiatingEntitySourceService.class,
        PaymentPoolConfig.class,
        DefaultTemplateProperties.class})
public class ShopManagementServiceTest {

    @MockBean
    KafkaTemplate<String, ReferenceInfo> kafkaTemplate;
    @MockBean
    private FraudResultRepository fraudResultRepository;
    @MockBean
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ShopManagementService shopManagementService;
    @Autowired
    private InitiatingEntitySourceService initiatingEntitySourceService;
    @Autowired
    private Pool<String> referencePoolImpl;
    @Autowired
    private Pool<String> groupReferencePoolImpl;
    @MockBean
    private FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> firstFindVisitor;
    @MockBean
    private CheckedResultFactory checkedResultFactory;

    @Test
    public void testCreateDefaultReference() {
        ReferenceInfo referenceInfo = ReferenceInfo.merchant_info(new MerchantInfo()
                .setPartyId("partyId")
                .setShopId("shopId_exists"));
        initiatingEntitySourceService.sendToSource(referenceInfo);
        verify(kafkaTemplate, times(1)).send(any(), any());
    }

    @Test
    public void testIsNewShop() {
        when(fraudResultRepository.isExistByField(anyString(), anyString(), anyLong(), anyLong())).thenReturn(false);
        shopManagementService.isNewShop("partyId", "s1");
        verify(fraudResultRepository).isExistByField(anyString(), anyString(), anyLong(), anyLong());

        referencePoolImpl.add("partyId", "test");
        boolean newShop = shopManagementService.isNewShop("partyId", "s1");
        assertFalse(newShop);

        referencePoolImpl.remove("partyId");
        when(fraudResultRepository.isExistByField(anyString(), anyString(), anyLong(), anyLong())).thenReturn(true);
        newShop = shopManagementService.isNewShop("partyId", "s1");
        assertTrue(newShop);
    }

}
