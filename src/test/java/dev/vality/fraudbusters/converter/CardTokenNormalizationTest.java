package dev.vality.fraudbusters.converter;

import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.damsel.proxy_inspector.Context;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.FraudRequest;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.util.BeanUtil;
import dev.vality.fraudbusters.util.PaymentTypeByContextResolver;
import org.junit.jupiter.api.Test;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CardTokenNormalizationTest {

    private final ContextToFraudRequestConverter contextConverter =
            new ContextToFraudRequestConverter(new PaymentTypeByContextResolver());
    private final PaymentToPaymentModelConverter historicalPaymentConverter =
            new PaymentToPaymentModelConverter();
    private final PaymentToCheckedPaymentConverter checkedPaymentConverter =
            new PaymentToCheckedPaymentConverter(new PaymentTypeByContextResolver());

    @Test
    void normalizesAbsentOnlineBankCardToUnknownToken() {
        Context context = BeanUtil.createContext();
        context.getPayment().getPayment().getPayer().getPaymentResource()
                .getResource().setPaymentTool(new PaymentTool());

        FraudRequest fraudRequest = contextConverter.convert(context);

        assertEquals(UNKNOWN, fraudRequest.getFraudModel().getCardToken());
    }

    @Test
    void normalizesMissingOnlineCardTokenToUnknown() {
        Context context = BeanUtil.createContext();
        context.getPayment().getPayment().getPayer().getPaymentResource()
                .getResource().getPaymentTool().getBankCard().unsetToken();

        FraudRequest fraudRequest = contextConverter.convert(context);

        assertEquals(UNKNOWN, fraudRequest.getFraudModel().getCardToken());
    }

    @Test
    void normalizesMissingHistoricalCardTokenToUnknown() {
        Payment payment = BeanUtil.createPayment(PaymentStatus.captured);
        payment.getPaymentTool().getBankCard().unsetToken();

        PaymentModel paymentModel = historicalPaymentConverter.convert(payment);

        assertEquals(UNKNOWN, paymentModel.getCardToken());
    }

    @Test
    void normalizesMissingStoredPaymentCardTokenToUnknown() {
        Payment payment = BeanUtil.createPayment(PaymentStatus.captured);
        payment.getPaymentTool().getBankCard().unsetToken();

        CheckedPayment checkedPayment = checkedPaymentConverter.convert(payment);

        assertEquals(UNKNOWN, checkedPayment.getCardToken());
    }

    @Test
    void preservesPresentCardToken() {
        String cardToken = "card-token";
        Context context = BeanUtil.createContext();
        context.getPayment().getPayment().getPayer().getPaymentResource()
                .getResource().getPaymentTool().getBankCard().setToken(cardToken);

        FraudRequest fraudRequest = contextConverter.convert(context);

        assertEquals(cardToken, fraudRequest.getFraudModel().getCardToken());
    }
}
