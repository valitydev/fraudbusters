package dev.vality.fraudbusters.util;

import dev.vality.damsel.fraudbusters.InspectUserContext;
import dev.vality.damsel.fraudbusters.ShopContext;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.stream.Collectors;

public class UserCacheKeyUtil {

    public static String buildInspectUserCacheKey(InspectUserContext context) {
        if (context == null) {
            return "null";
        }
        String email = ClickhouseUtilsValue.UNKNOWN;
        String phone = ClickhouseUtilsValue.UNKNOWN;
        if (context.getUserInfo() != null) {
            email = context.getUserInfo().isSetEmail() && StringUtils.hasLength(context.getUserInfo().getEmail())
                    ? context.getUserInfo().getEmail().toLowerCase()
                    : ClickhouseUtilsValue.UNKNOWN;
            phone = context.getUserInfo().getPhone() != null
                    && StringUtils.hasLength(context.getUserInfo().getPhone())
                    ? context.getUserInfo().getPhone()
                    : ClickhouseUtilsValue.UNKNOWN;
        }
        if (context.getShopList() == null || context.getShopList().isEmpty()) {
            return email + "|" + phone + "|";
        }
        String shopsKey = context.getShopList().stream()
                .map(UserCacheKeyUtil::buildShopKey)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
        return email + "|" + phone + "|" + shopsKey;
    }

    private static String buildShopKey(ShopContext shopContext) {
        if (shopContext == null || shopContext.getPartyId() == null || shopContext.getShopId() == null) {
            return ClickhouseUtilsValue.UNKNOWN;
        }
        String partyId = shopContext.getPartyId();
        String shopId = shopContext.getShopId();
        return partyId + ":" + shopId;
    }
}

