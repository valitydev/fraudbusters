package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.exception.UnknownFraudPaymentException;
import dev.vality.fraudbusters.service.PaymentInfoService;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static dev.vality.fraudbusters.constant.ClickhouseUtilsValue.UNKNOWN;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudPaymentToRowConverter implements Converter<FraudPayment, FraudPaymentRow> {

    private final PaymentInfoService paymentInfoService;

    @Override
    public FraudPaymentRow convert(FraudPayment fraudPayment) {
        LocalDateTime localDateTime = TimestampUtil.parseDate(fraudPayment.getEventTime());
        CheckedPayment checkedPayment =
                paymentInfoService.findPaymentByIdAndTimestamp(localDateTime.toLocalDate(), fraudPayment.getId());
        if (checkedPayment == null) {
            log.warn("Can't find payment for fraudPayment: {}", fraudPayment);
            throw new UnknownFraudPaymentException();
        }
        FraudPaymentRow payment = new FraudPaymentRow();
        payment.setTimestamp(checkedPayment.getTimestamp());
        payment.setEventTimeHour(checkedPayment.getEventTimeHour());
        payment.setEventTime(checkedPayment.getEventTime());
        payment.setId(checkedPayment.getId());
        payment.setEmail(
                StringUtils.hasText(checkedPayment.getEmail()) ? checkedPayment.getEmail().toLowerCase() : UNKNOWN);
        payment.setIp(checkedPayment.getIp());
        payment.setFingerprint(checkedPayment.getFingerprint());
        payment.setPhone(checkedPayment.getPhone());
        payment.setBin(checkedPayment.getBin());
        payment.setLastDigits(checkedPayment.getLastDigits());
        payment.setCardToken(checkedPayment.getCardToken());
        payment.setCardCategory(checkedPayment.getCardCategory());
        payment.setPaymentSystem(checkedPayment.getPaymentSystem());
        payment.setPaymentTool(checkedPayment.getPaymentTool());
        payment.setTerminal(checkedPayment.getTerminal());
        payment.setProviderId(checkedPayment.getProviderId());
        payment.setBankCountry(checkedPayment.getBankCountry());
        payment.setPartyId(checkedPayment.getPartyId());
        payment.setShopId(checkedPayment.getShopId());
        payment.setAmount(checkedPayment.getAmount());
        payment.setCurrency(checkedPayment.getCurrency());
        payment.setPaymentStatus(checkedPayment.getPaymentStatus());
        payment.setErrorCode(checkedPayment.getErrorCode());
        payment.setErrorReason(checkedPayment.getErrorReason());
        payment.setPayerType(checkedPayment.getPayerType());
        payment.setTokenProvider(checkedPayment.getTokenProvider());
        payment.setCheckedTemplate(checkedPayment.getCheckedTemplate());
        payment.setCheckedRule(checkedPayment.getCheckedRule());
        payment.setResultStatus(checkedPayment.getResultStatus());
        payment.setCheckedResultsJson(checkedPayment.getCheckedResultsJson());
        payment.setMobile(checkedPayment.isMobile());
        payment.setRecurrent(checkedPayment.isRecurrent());
        return payment;
    }

}
