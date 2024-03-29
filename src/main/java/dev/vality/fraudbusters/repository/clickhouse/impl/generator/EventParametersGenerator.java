package dev.vality.fraudbusters.repository.clickhouse.impl.generator;

import dev.vality.fraudbusters.constant.EventField;
import dev.vality.fraudbusters.constant.Separator;
import dev.vality.fraudbusters.domain.Event;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventParametersGenerator {

    @NotNull
    public static Map<String, Object> generateParamsByFraudModel(Event value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(EventField.timestamp.name(), value.getTimestamp());
        parameters.put(EventField.eventTimeHour.name(), value.getEventTimeHour());
        parameters.put(EventField.eventTime.name(), value.getEventTime());
        parameters.put(EventField.ip.name(), value.getIp());
        parameters.put(EventField.email.name(), value.getEmail());
        parameters.put(EventField.bin.name(), value.getBin());
        parameters.put(EventField.fingerprint.name(), value.getFingerprint());
        parameters.put(EventField.shopId.name(), value.getShopId());
        parameters.put(EventField.partyId.name(), value.getPartyId());
        parameters.put(EventField.resultStatus.name(), value.getResultStatus());
        parameters.put(EventField.amount.name(), value.getAmount());
        parameters.put(EventField.country.name(), value.getCountry());
        parameters.put(EventField.checkedRule.name(), value.getCheckedRule());
        Optional.ofNullable(value.getBankCountry()).ifPresent(v -> parameters.put(EventField.bankCountry.name(), v));
        Optional.ofNullable(value.getCurrency()).ifPresent(v -> parameters.put(EventField.currency.name(), v));
        Optional.ofNullable(value.getInvoiceId()).ifPresent(v -> parameters.put(EventField.invoiceId.name(), v));
        Optional.ofNullable(value.getLastDigits()).ifPresent(v -> parameters.put(EventField.lastDigits.name(), v));
        Optional.ofNullable(value.getInvoiceId())
                .ifPresent(v -> parameters.put(EventField.id.name(), v + Separator.DOT + value.getPaymentId()));
        Optional.ofNullable(value.getBankName()).ifPresent(v -> parameters.put(EventField.bankName.name(), v));
        Optional.ofNullable(value.getCardToken()).ifPresent(v -> parameters.put(EventField.cardToken.name(), v));
        Optional.ofNullable(value.getPaymentId()).ifPresent(v -> parameters.put(EventField.paymentId.name(), v));
        Optional.ofNullable(value.getCheckedTemplate())
                .ifPresent(v -> parameters.put(EventField.checkedTemplate.name(), v));
        Optional.ofNullable(value.getPayerType()).ifPresent(v -> parameters.put(EventField.payerType.name(), v));
        Optional.ofNullable(value.getTokenProvider())
                .ifPresent(v -> parameters.put(EventField.tokenProvider.name(), v));
        return parameters;
    }

}
