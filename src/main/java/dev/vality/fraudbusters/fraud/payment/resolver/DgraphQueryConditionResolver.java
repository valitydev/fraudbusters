package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudbusters.fraud.constant.DgraphEntity;
import dev.vality.fraudbusters.fraud.constant.DgraphFieldConditionTemplate;
import dev.vality.fraudbusters.fraud.constant.DgraphPaymentFilterField;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import org.springframework.stereotype.Service;

@Service
public class DgraphQueryConditionResolver {

    private static final String FILTER_POSTFIX = " @filter(%s)";
    private static final String DEFAULT_EXCEPTION = "Unknown object %s";

    public String resolvePaymentFilterByDgraphEntity(DgraphEntity entity) {
        String entityName = switch (entity) {
            case TOKEN -> DgraphPaymentFilterField.TOKEN;
            case BIN -> DgraphPaymentFilterField.BIN;
            case EMAIL -> DgraphPaymentFilterField.EMAIL;
            case FINGERPRINT -> DgraphPaymentFilterField.FINGERPRINT;
            case IP -> DgraphPaymentFilterField.IP;
            case PARTY -> DgraphPaymentFilterField.PARTY;
            case SHOP -> DgraphPaymentFilterField.SHOP;
            case COUNTRY -> DgraphPaymentFilterField.COUNTRY;
            case CURRENCY -> DgraphPaymentFilterField.CURRENCY;
            case FRAUD_PAYMENT -> DgraphPaymentFilterField.FRAUD_PAYMENT;
            case PAYMENT -> DgraphPaymentFilterField.PAYMENT;
            default -> throw new UnsupportedOperationException(String.format(DEFAULT_EXCEPTION, entity));
        };
        return entityName + FILTER_POSTFIX;
    }

    public String resolveConditionByPaymentCheckedField(PaymentCheckedField paymentCheckedField,
                                                        PaymentModel paymentModel) {
        return switch (paymentCheckedField) {
            case BIN -> String.format(DgraphFieldConditionTemplate.BIN, paymentModel.getBin());
            case EMAIL -> String.format(DgraphFieldConditionTemplate.EMAIL, paymentModel.getEmail());
            case IP, COUNTRY_IP -> String.format(DgraphFieldConditionTemplate.IP, paymentModel.getIp());
            case FINGERPRINT -> String.format(DgraphFieldConditionTemplate.FINGERPRINT, paymentModel.getFingerprint());
            case CARD_TOKEN -> String.format(DgraphFieldConditionTemplate.CARD_TOKEN, paymentModel.getCardToken());
            case PARTY_ID -> String.format(DgraphFieldConditionTemplate.PARTY_ID, paymentModel.getPartyId());
            case SHOP_ID -> String.format(DgraphFieldConditionTemplate.SHOP_ID, paymentModel.getShopId());
            case LAST_DIGITS -> String.format(DgraphFieldConditionTemplate.LAST_DIGITS, paymentModel.getLastDigits());
            case COUNTRY_BANK ->
                    String.format(DgraphFieldConditionTemplate.COUNTRY_BANK, paymentModel.getBinCountryCode());
            case CURRENCY -> String.format(DgraphFieldConditionTemplate.CURRENCY, paymentModel.getCurrency());
            case MOBILE -> String.format(DgraphFieldConditionTemplate.MOBILE, paymentModel.isMobile());
            case RECURRENT -> String.format(DgraphFieldConditionTemplate.RECURRENT, paymentModel.isRecurrent());
            default -> throw new UnsupportedOperationException(String.format(DEFAULT_EXCEPTION, paymentCheckedField));
        };
    }

}
