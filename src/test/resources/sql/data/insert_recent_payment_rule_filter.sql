INSERT INTO fraud.payment
(timestamp, eventTime, eventTimeHour, partyId, shopId, email, fingerprint, amount, currency, status, errorReason, id,
 ip, bin, lastDigits, paymentTool, cardToken, paymentSystem, terminal, providerId, bankCountry, errorCode,
 tokenProvider)
VALUES (today(), toUInt64(toUnixTimestamp(now())), toUInt64(toUnixTimestamp(toStartOfHour(now()))) * 1000,
        'rule_filter_party', 'rule_filter_shop', 'rule_filter_email', 'rule_filter_fingerprint',
        50000, 'RUB', 'captured', '', 'rule-filter-payment.1', '127.0.0.1', '666', '3125', 'bank_card',
        'rule_filter_card_token', 'VISA', '123', '1', 'RUS', '', 'tprovider');

INSERT INTO fraud.events_unique
(timestamp, eventTime, eventTimeHour, partyId, shopId, email, fingerprint, amount, currency, mobile, id, paymentId,
 ip, bin, lastDigits, cardToken, resultStatus, checkedRule, checkedTemplate, bankCountry, invoiceId, bankName)
VALUES (today(), toUInt64(toUnixTimestamp(now())), toUInt64(toUnixTimestamp(toStartOfHour(now()))) * 1000,
        'rule_filter_party', 'rule_filter_shop', 'rule_filter_email', 'rule_filter_fingerprint',
        50000, 'RUB', 0, 'rule-filter-payment.1', '1', '127.0.0.1', '666', '3125',
        'rule_filter_card_token', 'DECLINE', '3DS_RULE', '3DS_TEMPLATE', 'RUS', 'rule-filter-invoice', 'SBER');
