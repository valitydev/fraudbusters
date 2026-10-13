package dev.vality.fraudbusters.config;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.service.ShopManagementService;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ExternalServiceConfig.class, ShopManagementService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockExternalServices
class ExternalServiceMockOverrideTest {

    @Autowired
    private ColumbusServiceSrv.Iface geoIpServiceSrv;
    @Autowired
    private WbListServiceSrv.Iface wbListServiceSrv;
    @Autowired
    private TrustedTokensSrv.Iface trustedTokensSrv;
    @Autowired
    private ShopManagementService shopManagementService;

    @Test
    void replacesProductionBeansWithoutEnablingBeanDefinitionOverriding() throws Exception {
        assertTrue(mockingDetails(geoIpServiceSrv).isMock());
        assertTrue(mockingDetails(wbListServiceSrv).isMock());
        assertTrue(mockingDetails(trustedTokensSrv).isMock());
        assertTrue(mockingDetails(shopManagementService).isMock());

        when(geoIpServiceSrv.getLocationIsoCode(any())).thenReturn("RUS");
        assertEquals("RUS", geoIpServiceSrv.getLocationIsoCode("127.0.0.1"));
    }
}
