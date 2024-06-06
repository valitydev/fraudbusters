package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.ClientInfo;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.util.TimestampUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;

@Component
public class PaymentToPaymentModelConverter implements Converter<Payment, PaymentModel> {

    @Override
    @NonNull
    public PaymentModel convert(Payment payment) {
        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPartyId(payment.getReferenceInfo().getMerchantInfo().getPartyId());
        paymentModel.setShopId(payment.getReferenceInfo().getMerchantInfo().getShopId());
        paymentModel.setBin(payment.getPaymentTool().getBankCard().getBin());
        paymentModel.setBinCountryCode(payment.getPaymentTool().getBankCard().isSetIssuerCountry()
                ? payment.getPaymentTool().getBankCard().getIssuerCountry().name()
                : UNKNOWN);
        ClientInfo clientInfo = payment.getClientInfo();
        paymentModel.setIp(clientInfo.isSetIp() ? clientInfo.getIp() : ClickhouseUtilsValue.UNKNOWN);
        paymentModel.setFingerprint(
                clientInfo.isSetFingerprint() ? clientInfo.getFingerprint() : ClickhouseUtilsValue.UNKNOWN);
        paymentModel.setAmount(payment.getCost().getAmount());
        paymentModel.setCurrency(payment.getCost().getCurrency().getSymbolicCode());
        paymentModel.setMobile(payment.isMobile());
        paymentModel.setRecurrent(payment.isRecurrent());
        paymentModel.setCardToken(payment.getPaymentTool().getBankCard().getToken());
        paymentModel.setLastDigits(payment.getPaymentTool().getBankCard().getLastDigits());
        paymentModel.setTimestamp(TimestampUtil.parseInstantFromString(payment.getEventTime()).toEpochMilli());
        return paymentModel;
    }

}
