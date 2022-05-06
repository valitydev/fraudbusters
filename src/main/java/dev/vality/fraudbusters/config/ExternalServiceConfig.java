package dev.vality.fraudbusters.config;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class ExternalServiceConfig {

    @Bean
    public ColumbusServiceSrv.Iface geoIpServiceSrv(
            @Value("${geo.ip.service.url}") Resource url,
            @Value("${geo.ip.service.timeout:5000}") Integer timeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(url.getURI())
                .withNetworkTimeout(timeout)
                .build(ColumbusServiceSrv.Iface.class);
    }

    @Bean
    public WbListServiceSrv.Iface wbListServiceSrv(
            @Value("${wb.list.service.url}") Resource url,
            @Value("${wb.list.service.timeout:5000}") Integer timeout)
            throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(url.getURI())
                .withNetworkTimeout(timeout)
                .build(WbListServiceSrv.Iface.class);
    }

    @Bean
    public TrustedTokensSrv.Iface trustedTokensSrv(@Value("${trusted.tokens.url}") Resource url,
                                                   @Value("${trusted.tokens.timeout:5000}") Integer timeout)
            throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(url.getURI())
                .withNetworkTimeout(timeout)
                .build(TrustedTokensSrv.Iface.class);
    }

}
