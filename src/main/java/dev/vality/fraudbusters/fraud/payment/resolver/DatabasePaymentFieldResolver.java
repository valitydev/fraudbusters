package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudbusters.constant.EventField;
import dev.vality.fraudbusters.exception.UnknownFieldException;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabasePaymentFieldResolver {

    @NotNull
    public List<FieldModel> resolveListFields(PaymentModel model, List<PaymentCheckedField> list) {
        if (list != null) {
            return list.stream()
                    .map(field -> resolve(field, model))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public FieldModel resolve(PaymentCheckedField field, PaymentModel model) {
        if (field == null) {
            throw new UnknownFieldException();
        }
        return switch (field) {
            case IP, COUNTRY_IP -> new FieldModel(EventField.ip.name(), model.getIp());
            case EMAIL -> new FieldModel(EventField.email.name(), model.getEmail());
            case BIN -> new FieldModel(EventField.bin.name(), model.getBin());
            case FINGERPRINT -> new FieldModel(EventField.fingerprint.name(), model.getFingerprint());
            case PARTY_ID -> new FieldModel(EventField.partyId.name(), model.getPartyId());
            case SHOP_ID -> new FieldModel(EventField.shopId.name(), model.getShopId());
            case CARD_TOKEN -> new FieldModel(EventField.cardToken.name(), model.getCardToken());
            case LAST_DIGITS -> new FieldModel(EventField.lastDigits.name(), model.getLastDigits());
            case MOBILE -> new FieldModel(EventField.mobile.name(), model.isMobile());
            case RECURRENT -> new FieldModel(EventField.recurrent.name(), model.isRecurrent());
            case CURRENCY -> new FieldModel(EventField.currency.name(), model.getCurrency());
            case PHONE -> new FieldModel(EventField.phone.name(), model.getPhone());
            case COUNTRY_BANK -> new FieldModel(EventField.bankCountry.name(), model.getBinCountryCode());
            default -> throw new UnknownFieldException();
        };
    }

    public String resolve(PaymentCheckedField field) {
        if (field == null) {
            throw new UnknownFieldException();
        }
        return switch (field) {
            case IP -> EventField.ip.name();
            case EMAIL -> EventField.email.name();
            case PHONE -> EventField.phone.name();
            case BIN -> EventField.bin.name();
            case LAST_DIGITS -> EventField.lastDigits.name();
            case FINGERPRINT -> EventField.fingerprint.name();
            case PARTY_ID -> EventField.partyId.name();
            case SHOP_ID -> EventField.shopId.name();
            case CARD_TOKEN -> EventField.cardToken.name();
            case MOBILE -> EventField.mobile.name();
            case RECURRENT -> EventField.recurrent.name();
            default -> throw new UnknownFieldException();
        };
    }

}
