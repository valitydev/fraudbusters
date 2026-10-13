package dev.vality.fraudbusters.config;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.service.ShopManagementService;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class MockExternalServiceConfig {

    @Bean
    public ColumbusServiceSrv.Iface geoIpServiceSrv() {
        return mock(ColumbusServiceSrv.Iface.class);
    }

    @Bean
    public WbListServiceSrv.Iface wbListServiceSrv() {
        return mock(WbListServiceSrv.Iface.class);
    }

    @Bean
    public TrustedTokensSrv.Iface trustedTokensSrv() {
        return mock(TrustedTokensSrv.Iface.class);
    }

    @Bean
    public ShopManagementService shopManagementService() {
        return mock(ShopManagementService.class);
    }

}
