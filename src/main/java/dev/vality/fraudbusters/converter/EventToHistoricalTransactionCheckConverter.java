package dev.vality.fraudbusters.converter;

import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.CurrencyRef;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudbusters.domain.Event;
import dev.vality.fraudbusters.util.TimestampUtil;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class EventToHistoricalTransactionCheckConverter implements Converter<Event, HistoricalTransactionCheck> {

    public static final String EMPTY_FILED = "-";
    private final ResultStatusConverter resultStatusConverter;

    @Override
    public HistoricalTransactionCheck convert(Event event) {
        HistoricalTransactionCheck historicalTransactionCheck = new HistoricalTransactionCheck();
        Payment payment = convertPayment(event);
        historicalTransactionCheck.setTransaction(payment);
        CheckResult checkResult = convertCheckResult(event);
        historicalTransactionCheck.setCheckResult(checkResult);
        return historicalTransactionCheck;
    }

    @NotNull
    private CheckResult convertCheckResult(Event event) {
        CheckResult checkResult = new CheckResult();
        checkResult.setCheckedTemplate(event.getCheckedTemplate());
        ConcreteCheckResult concreteCheckResult = new ConcreteCheckResult();
        concreteCheckResult.setNotificationsRule(Collections.emptyList());
        concreteCheckResult.setRuleChecked(event.getCheckedRule());
        ResultStatus resultStatus = ResultStatus.valueOf(event.getResultStatus());
        concreteCheckResult.setResultStatus(resultStatusConverter.convert(resultStatus));
        checkResult.setConcreteCheckResult(concreteCheckResult);
        return checkResult;
    }

    private Payment convertPayment(Event event) {
        ReferenceInfo referenceInfo = new ReferenceInfo();
        referenceInfo.setMerchantInfo(
                new MerchantInfo()
                        .setPartyId(event.getPartyId())
                        .setShopId(event.getShopId()));
        PaymentTool paymentTool = new PaymentTool();
        BankCard bankCard = new BankCard();
        paymentTool.setBankCard(bankCard);
        bankCard.setToken(event.getCardToken());
        bankCard.setBankName(event.getBankName());
        bankCard.setBin(event.getBin());
        bankCard.setLastDigits(event.getLastDigits());
        return new Payment()
                .setId(event.getPaymentId())
                .setEventTime(TimestampUtil.getStringDate(event.getEventTime()))
                .setClientInfo(new ClientInfo()
                        .setFingerprint(event.getFingerprint())
                        .setIp(event.getIp())
                        .setEmail(event.getEmail()))
                .setReferenceInfo(referenceInfo)
                .setCost(new Cash()
                        .setAmount(event.getAmount())
                        .setCurrency(new CurrencyRef()
                                .setSymbolicCode(event.getCurrency())))
                .setPaymentTool(paymentTool)
                .setMobile(event.isMobile())
                .setRecurrent(event.isRecurrent())
                .setProviderInfo(new ProviderInfo()
                        .setProviderId(EMPTY_FILED)
                        .setCountry(EMPTY_FILED)
                        .setTerminalId(EMPTY_FILED))
                .setStatus(PaymentStatus.unknown);
    }
}
