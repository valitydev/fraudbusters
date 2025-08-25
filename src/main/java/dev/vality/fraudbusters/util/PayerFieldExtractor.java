package dev.vality.fraudbusters.util;

import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.ClientInfo;
import dev.vality.damsel.domain.ContactInfo;
import dev.vality.damsel.domain.Payer;
import dev.vality.fraudbusters.constant.ClickhouseUtilsValue;
import dev.vality.fraudbusters.constant.PayerType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PayerFieldExtractor {

    public static Optional<ContactInfo> getContactInfo(Payer payer) {
        if (payer.isSetPaymentResource()) {
            return Optional.ofNullable(payer.getPaymentResource().getContactInfo());
        } else if (payer.isSetRecurrent()) {
            return Optional.ofNullable(payer.getRecurrent().getContactInfo());
        }
        return Optional.empty();
    }

    public static Optional<BankCard> getBankCard(Payer payer) {
        if (payer.isSetPaymentResource()
                    && payer.getPaymentResource().getResource().getPaymentTool().isSetBankCard()) {
            return Optional.ofNullable(payer.getPaymentResource().getResource().getPaymentTool().getBankCard());
        } else if (payer.isSetRecurrent() && payer.getRecurrent().getPaymentTool().isSetBankCard()) {
            return Optional.ofNullable(payer.getRecurrent().getPaymentTool().getBankCard());
        }
        return Optional.empty();
    }

    public static Optional<ClientInfo> getClientInfo(Payer payer) {
        if (payer.isSetPaymentResource()) {
            return Optional.ofNullable(payer.getPaymentResource().getResource().getClientInfo());
        }
        return Optional.empty();
    }

    public static String getPayerType(Payer payer) {
        if (payer.isSetPaymentResource()) {
            return PayerType.PAYMENT_RESOURCE.name();
        } else if (payer.isSetRecurrent()) {
            return PayerType.RECURRENT.name();
        } else {
            return ClickhouseUtilsValue.UNKNOWN;
        }
    }
}
