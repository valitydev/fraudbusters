package dev.vality.fraudbusters.repository.clickhouse.query;

import dev.vality.fraudbusters.constant.EventSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargeBackQuery {

    public static final String SELECT_HISTORY_CHARGEBACK = String.format("""
            SELECT
                        eventTime,
                        partyId,
                        shopId,
                        email,
                        amount as amount,
                        currency,
                        id,
                        bankCountry,
                        cardToken,
                        bin,
                        lastDigits,
                        paymentSystem,
                        providerId,
                        status,
                        ip,
                        fingerprint,
                        terminal,
                        paymentId,
                        chargebackCode,
                        category
                     FROM
            %s
             WHERE
                timestamp >= toDate(:from)
                and timestamp <= toDate(:to)
                and toDateTime(eventTime) >= toDateTime(:from)
                and toDateTime(eventTime) <= toDateTime(:to)
                and shopId != 'TEST'""",
            EventSource.FRAUD_EVENTS_CHARGEBACK.getTable());
}
