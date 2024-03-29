package dev.vality.fraudbusters.repository.clickhouse.setter;

import dev.vality.fraudbusters.domain.CheckedPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
public class PaymentBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    public static final String FIELDS = """
            timestamp, eventTimeHour, eventTime,
            id,
            email, phone, ip, fingerprint,
            bin, lastDigits, cardToken, cardCategory, paymentSystem, paymentTool,
            terminal, providerId, bankCountry,
            partyId, shopId,
            amount, currency,
            status, errorCode, errorReason,
            payerType, tokenProvider,
            checkedTemplate, checkedRule, resultStatus, checkedResultsJson, mobile,
            recurrent
            """;

    public static final String FIELDS_MARK = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";

    private final List<CheckedPayment> batch;

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        CheckedPayment checkedPayment = batch.get(i);
        int l = 1;
        ps.setObject(l++, checkedPayment.getTimestamp());
        ps.setLong(l++, checkedPayment.getEventTimeHour());
        ps.setLong(l++, checkedPayment.getEventTime());
        ps.setString(l++, checkedPayment.getId());
        ps.setString(l++, checkedPayment.getEmail());
        ps.setString(l++, checkedPayment.getPhone());
        ps.setString(l++, checkedPayment.getIp());
        ps.setString(l++, checkedPayment.getFingerprint());

        ps.setString(l++, checkedPayment.getBin());
        ps.setString(l++, checkedPayment.getLastDigits());
        ps.setString(l++, checkedPayment.getCardToken());
        ps.setString(l++, checkedPayment.getCardCategory());
        ps.setString(l++, checkedPayment.getPaymentSystem());
        ps.setString(l++, checkedPayment.getPaymentTool());

        ps.setString(l++, checkedPayment.getTerminal());
        ps.setString(l++, checkedPayment.getProviderId());
        ps.setString(l++, checkedPayment.getBankCountry());

        ps.setString(l++, checkedPayment.getPartyId());
        ps.setString(l++, checkedPayment.getShopId());

        ps.setLong(l++, checkedPayment.getAmount());
        ps.setString(l++, checkedPayment.getCurrency());

        ps.setObject(l++, checkedPayment.getPaymentStatus());
        ps.setObject(l++, checkedPayment.getErrorCode());
        ps.setObject(l++, checkedPayment.getErrorReason());

        ps.setObject(l++, checkedPayment.getPayerType());
        ps.setObject(l++, checkedPayment.getTokenProvider());
        ps.setObject(l++, checkedPayment.getCheckedTemplate());
        ps.setObject(l++, checkedPayment.getCheckedRule());
        ps.setObject(l++, checkedPayment.getResultStatus());
        ps.setObject(l++, checkedPayment.getCheckedResultsJson());
        ps.setObject(l++, checkedPayment.isMobile());
        ps.setObject(l, checkedPayment.isRecurrent());
    }

    @Override
    public int getBatchSize() {
        return batch.size();
    }
}
