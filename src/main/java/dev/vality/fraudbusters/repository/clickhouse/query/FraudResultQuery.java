package dev.vality.fraudbusters.repository.clickhouse.query;

import dev.vality.fraudbusters.constant.EventSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FraudResultQuery {

    private static final String HISTORY_FRAUD_RESULT_SOURCE = String.format("""
                    FROM
                    %s
                    WHERE
                        timestamp >= toDate(:from)
                        and timestamp <= toDate(:to)
                        and toDateTime(eventTime) >= toDateTime(:from)
                        and toDateTime(eventTime) <= toDateTime(:to)
                        and shopId != 'TEST'""",
            EventSource.FRAUD_EVENTS_UNIQUE.getTable());

    public static final String SELECT_HISTORY_FRAUD_RESULT_IDS = "SELECT id\n" + HISTORY_FRAUD_RESULT_SOURCE;

    public static final String SELECT_HISTORY_FRAUD_RESULT = """
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
                        lastDigits,
                        bin,
                        bankName,
                        paymentId,
                        resultStatus,
                        checkedRule,
                        checkedTemplate,
                        mobile,
                        recurrent
                    """ + HISTORY_FRAUD_RESULT_SOURCE;
}
