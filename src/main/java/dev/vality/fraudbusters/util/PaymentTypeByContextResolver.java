package dev.vality.fraudbusters.util;

import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.Payer;
import org.springframework.stereotype.Component;

@Component
public class PaymentTypeByContextResolver {

    public boolean isRecurrent(Payer payer) {
        return payer.isSetRecurrent() || payer.isSetCustomer();
    }

    public boolean isMobile(BankCard bankCard) {
        return bankCard.getPaymentToken() != null;
    }

}
