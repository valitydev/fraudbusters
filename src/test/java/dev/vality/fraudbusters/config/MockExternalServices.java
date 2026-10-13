package dev.vality.fraudbusters.config;

import dev.vality.columbus.ColumbusServiceSrv;
import dev.vality.damsel.wb_list.WbListServiceSrv;
import dev.vality.fraudbusters.service.ShopManagementService;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@MockitoBean(types = {ColumbusServiceSrv.Iface.class, WbListServiceSrv.Iface.class,
        TrustedTokensSrv.Iface.class, ShopManagementService.class})
public @interface MockExternalServices {
}
