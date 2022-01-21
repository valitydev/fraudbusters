package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.FraudPaymentInfo;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckedPaymentToFraudPaymentInfoConverter implements Converter<CheckedPayment, FraudPaymentInfo> {

    private final CheckedPaymentToPaymentConverter paymentConverter;

    @Override
    public FraudPaymentInfo convert(CheckedPayment checkedPayment) {
        Payment payment = paymentConverter.convert(checkedPayment);
        FraudPaymentRow fraudPayment = (FraudPaymentRow) checkedPayment;
        FraudPaymentInfo fraudPaymentInfo = new FraudPaymentInfo();
        fraudPaymentInfo.setPayment(payment);
        fraudPaymentInfo.setComment(fraudPayment.getComment());
        fraudPaymentInfo.setType(fraudPayment.getType());
        return fraudPaymentInfo;
    }

}
