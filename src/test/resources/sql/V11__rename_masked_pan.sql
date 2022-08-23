ALTER TABLE fraud.events_unique RENAME COLUMN maskedPan TO lastDigits;
ALTER TABLE fraud.fraud_payment RENAME COLUMN maskedPan TO lastDigits;
ALTER TABLE fraud.refund RENAME COLUMN maskedPan TO lastDigits;
ALTER TABLE fraud.payment RENAME COLUMN maskedPan TO lastDigits;
ALTER TABLE fraud.chargeback RENAME COLUMN maskedPan TO lastDigits;
ALTER TABLE fraud.withdrawal RENAME COLUMN maskedPan TO lastDigits;
