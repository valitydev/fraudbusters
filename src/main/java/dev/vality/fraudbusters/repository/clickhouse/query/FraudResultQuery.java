package dev.vality.fraudbusters.repository.clickhouse.query;

import dev.vality.fraudbusters.constant.EventSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FraudResultQuery {

    public static final String SELECT_HISTORY_FRAUD_RESULT = String.format("""
                    SELECT
                        eventTime,
                        partyId,
                        shopId,
                        email,
                        amount as amount,
                        currency,
                        bankCountry,
                        cardToken,
                        ip,
                        fingerprint,
                        id,
                        invoiceId,
                        maskedPan,
                        bin,
                        bankName,
                        paymentId,
                        resultStatus,
                        checkedRule,
                        checkedTemplate,
                        mobile,
                        recurrent
                     FROM
                    %s
                     WHERE
                        timestamp >= toDate(:from)
                        and timestamp <= toDate(:to)
                        and toDateTime(eventTime) >= toDateTime(:from)
                        and toDateTime(eventTime) <= toDateTime(:to)
                        and shopId != 'TEST'""",
            EventSource.FRAUD_EVENTS_UNIQUE.getTable());
}
