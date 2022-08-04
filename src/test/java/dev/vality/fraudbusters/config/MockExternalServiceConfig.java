package dev.vality.fraudbusters.config;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.service.ShopManagementService;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockExternalServiceConfig {

    @MockBean
    ColumbusServiceSrv.Iface geoIpServiceSrv;
    @MockBean
    WbListServiceSrv.Iface wbListServiceSrv;
    @MockBean
    TrustedTokensSrv.Iface trustedTokensSrv;
    @MockBean
    ShopManagementService shopManagementService;


}
