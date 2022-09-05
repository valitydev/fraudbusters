package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudo.exception.UnresolvableFieldException;
import dev.vality.fraudo.resolver.FieldResolver;

public class PaymentModelFieldResolver implements FieldResolver<PaymentModel, PaymentCheckedField> {

    @Override
    public String resolveValue(String fieldName, PaymentModel paymentModel) {
        return switch (PaymentCheckedField.getByValue(fieldName)) {
            case BIN -> paymentModel.getBin();
            case IP -> paymentModel.getIp();
            case FINGERPRINT -> paymentModel.getFingerprint();
            case EMAIL -> paymentModel.getEmail();
            case COUNTRY_BANK -> paymentModel.getBinCountryCode();
            case CARD_TOKEN -> paymentModel.getCardToken();
            case LAST_DIGITS -> paymentModel.getLastDigits();
            case MASKED_PAN -> paymentModel.getBin() + paymentModel.getLastDigits();
            default -> throw new UnresolvableFieldException(fieldName);
        };
    }

    @Override
    public PaymentCheckedField resolveName(String fieldName) {
        return PaymentCheckedField.getByValue(fieldName);
    }

}
