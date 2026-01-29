package dev.vality.fraudbusters.util;

import dev.vality.damsel.fraudbusters.InspectUserContext;
import dev.vality.damsel.fraudbusters.ShopContext;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.fraud.model.PaymentModel;

public class PaymentModelFactory {

    public static PaymentModel buildPaymentModel(InspectUserContext context, ShopContext shopContext) {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPartyId(shopContext.getPartyId());
        paymentModel.setShopId(shopContext.getShopId());
        paymentModel.setTimestamp(System.currentTimeMillis());
        if (context.getUserInfo() != null) {
            paymentModel.setEmail(context.getUserInfo().getEmail().isPresent()
                    ? context.getUserInfo().getEmail().get()
                    : ClickhouseUtilsValue.UNKNOWN);
            paymentModel.setPhone(context.getUserInfo().getPhone().isPresent()
                    ? context.getUserInfo().getPhone().get()
                    : ClickhouseUtilsValue.UNKNOWN
            );
        } else {
            paymentModel.setEmail(ClickhouseUtilsValue.UNKNOWN);
            paymentModel.setPhone(ClickhouseUtilsValue.UNKNOWN);
        }
        return paymentModel;
    }
}

