package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.constant.EventSource;
import dev.vality.fraudbusters.constant.PaymentStatus;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.repository.clickhouse.extractor.CheckedPaymentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInfoService {

    private static final String FIELDS = """
            timestamp, eventTimeHour, eventTime, id, email, phone, ip, fingerprint, bin,
            lastDigits, cardToken, cardCategory, paymentSystem, paymentTool, terminal, 
            providerId, bankCountry, partyId, shopId, amount, currency, status, 
            errorCode, errorReason,
            payerType, tokenProvider, checkedTemplate, checkedRule, resultStatus,
            checkedResultsJson, mobile, recurrent
            """;

    private final JdbcTemplate jdbcTemplate;

    public CheckedPayment findPaymentByIdAndTimestamp(LocalDate timestamp, String id) {
        log.debug("findPaymentByIdAndTimestamp timestamp: {} id: {}", timestamp, id);
        return jdbcTemplate.query("select " + FIELDS + " from " + EventSource.FRAUD_EVENTS_PAYMENT.getTable() +
                                  " where timestamp = ? and id = ? and status = ? ",
                List.of(timestamp, id, PaymentStatus.captured.name()).toArray(), new CheckedPaymentExtractor()
        );
    }

}
