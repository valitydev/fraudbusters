package dev.vality.fraudbusters.converter;

import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.fraudbusters.Error;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudbusters.constant.PaymentToolType;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.TimeProperties;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.geck.common.util.TBaseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;

@Component
@RequiredArgsConstructor
public class PaymentToCheckedPaymentConverter implements Converter<Payment, CheckedPayment> {

    private final PaymentTypeByContextResolver paymentTypeByContextResolver;

    @NonNull
    @Override
    public CheckedPayment convert(Payment payment) {
        CheckedPayment checkedPayment = new CheckedPayment();
        TimeProperties timeProperties = TimestampUtil.generateTimePropertiesByString(payment.getEventTime());
        checkedPayment.setTimestamp(timeProperties.getTimestamp());
        checkedPayment.setEventTimeHour(timeProperties.getEventTimeHour());
        checkedPayment.setEventTime(timeProperties.getEventTime());
        checkedPayment.setId(payment.getId());

        ClientInfo clientInfo = payment.getClientInfo();
        checkedPayment.setEmail(clientInfo.isSetEmail() ? clientInfo.getEmail() : UNKNOWN);
        checkedPayment.setPhone(clientInfo.isSetPhone() ? clientInfo.getPhone() : UNKNOWN);
        checkedPayment.setIp(clientInfo.isSetIp() ? clientInfo.getIp() : UNKNOWN);
        checkedPayment.setFingerprint(clientInfo.isSetFingerprint() ? clientInfo.getFingerprint() : UNKNOWN);

        PaymentTool paymentTool = payment.getPaymentTool();
        checkedPayment.setPaymentTool(TBaseUtil.unionFieldToEnum(paymentTool, PaymentToolType.class).name());
        checkedPayment.setBin(paymentTool.isSetBankCard() ? paymentTool.getBankCard().getBin() : UNKNOWN);
        checkedPayment.setCardCategory(paymentTool.isSetBankCard() ? paymentTool.getBankCard().getCategory() : UNKNOWN);
        checkedPayment.setLastDigits(paymentTool.isSetBankCard() ? paymentTool.getBankCard().getLastDigits() : UNKNOWN);
        checkedPayment.setCardToken(paymentTool.isSetBankCard() ? paymentTool.getBankCard().getToken() : UNKNOWN);
        checkedPayment.setPaymentSystem(paymentTool.isSetBankCard()
                ? paymentTool.getBankCard().payment_system.getId()
                : UNKNOWN);

        ProviderInfo providerInfo = payment.getProviderInfo();
        checkedPayment.setTerminal(providerInfo.getTerminalId());
        checkedPayment.setProviderId(providerInfo.getProviderId());
        checkedPayment.setBankCountry(providerInfo.getCountry());

        ReferenceInfo referenceInfo = payment.getReferenceInfo();
        MerchantInfo merchantInfo = payment.getReferenceInfo().getMerchantInfo();
        checkedPayment.setPartyId(referenceInfo.isSetMerchantInfo() ? merchantInfo.getPartyId() : UNKNOWN);
        checkedPayment.setShopId(referenceInfo.isSetMerchantInfo() ? merchantInfo.getShopId() : UNKNOWN);

        checkedPayment.setAmount(payment.getCost().getAmount());
        checkedPayment.setCurrency(payment.getCost().getCurrency().getSymbolicCode());

        checkedPayment.setPaymentStatus(payment.getStatus().name());

        Error error = payment.getError();
        checkedPayment.setErrorCode(error == null ? null : error.getErrorCode());
        checkedPayment.setErrorReason(error == null ? null : error.getErrorReason());

        checkedPayment.setPayerType(payment.isSetPayerType() ? payment.getPayerType().name() : UNKNOWN);
        checkedPayment.setTokenProvider(paymentTool.isSetBankCard()
                && paymentTypeByContextResolver.isMobile(paymentTool.getBankCard())
                ? paymentTool.getBankCard().getPaymentToken().getId()
                : UNKNOWN);
        checkedPayment.setMobile(payment.isMobile());
        checkedPayment.setRecurrent(payment.isRecurrent());
        return checkedPayment;
    }

}
