package dev.vality.fraudbusters.repository.clickhouse.query;

import dev.vality.fraudbusters.constant.EventSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RefundQuery {

    public static final String SELECT_HISTORY_REFUND = String.format("""
                    SELECT
                        eventTime,
                        partyId,
                        shopId,
                        email,
                        amount as amount,
                        currency,
                        id,
                        cardToken,
                        bin,
                        lastDigits,
                        bankCountry,
                        fingerprint,
                        ip,
                        status,
                        errorReason,
                        errorCode,
                        paymentSystem,
                        providerId,
                        terminal,
                        paymentId
             FROM
             %s
             WHERE
                timestamp >= toDate(:from)
                and timestamp <= toDate(:to)
                and toDateTime(eventTime) >= toDateTime(:from)
                and toDateTime(eventTime) <= toDateTime(:to)
                and shopId != 'TEST'""",
            EventSource.FRAUD_EVENTS_REFUND.getTable());
}
