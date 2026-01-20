package dev.vality.fraudbusters.util;

import dev.vality.damsel.proxy_inspector.InspectUserContext;
import dev.vality.damsel.proxy_inspector.ShopContext;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import org.springframework.util.StringUtils;

public class PaymentModelFactory {

    public static PaymentModel buildPaymentModel(InspectUserContext context, ShopContext shopContext) {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPartyId(shopContext.getParty().getPartyRef().getId());
        paymentModel.setShopId(shopContext.getShop().getShopRef().getId());
        paymentModel.setTimestamp(System.currentTimeMillis());
        if (context.getUserInfo() != null) {
            paymentModel.setEmail(
                    context.getUserInfo().isSetEmail() && StringUtils.hasLength(context.getUserInfo().getEmail())
                            ? context.getUserInfo().getEmail().toLowerCase()
                            : ClickhouseUtilsValue.UNKNOWN);
            paymentModel.setPhone(
                    context.getUserInfo().isSetPhoneNumber()
                            && StringUtils.hasLength(context.getUserInfo().getPhoneNumber())
                            ? context.getUserInfo().getPhoneNumber()
                            : ClickhouseUtilsValue.UNKNOWN
            );
        } else {
            paymentModel.setEmail(ClickhouseUtilsValue.UNKNOWN);
            paymentModel.setPhone(ClickhouseUtilsValue.UNKNOWN);
        }
        return paymentModel;
    }
}

