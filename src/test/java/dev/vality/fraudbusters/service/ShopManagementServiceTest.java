package dev.vality.fraudbusters.service;

import dev.vality.damsel.fraudbusters.MerchantInfo;
import dev.vality.damsel.fraudbusters.ReferenceInfo;
import dev.vality.fraudbusters.config.RestTemplateConfig;
import dev.vality.fraudbusters.config.properties.DefaultTemplateProperties;
import dev.vality.fraudbusters.repository.clickhouse.impl.AggregationGeneralRepositoryImpl;
import dev.vality.fraudbusters.repository.clickhouse.impl.FraudResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {ShopManagementService.class,
        AggregationGeneralRepositoryImpl.class,
        FraudResultRepository.class,
        RestTemplateConfig.class,
        InitiatingEntitySourceService.class,
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
        when(fraudResultRepository.countOperationByField(anyString(), anyString(), anyLong(), anyLong())).thenReturn(0);
        shopManagementService.isNewShop("s1");
        verify(fraudResultRepository).countOperationByField(anyString(), anyString(), anyLong(), anyLong());
    }

}
