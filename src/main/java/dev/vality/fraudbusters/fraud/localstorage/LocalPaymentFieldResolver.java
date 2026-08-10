package dev.vality.fraudbusters.fraud.localstorage;

import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.FieldModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.resolver.DatabasePaymentFieldResolver;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalPaymentFieldResolver {

    private final DatabasePaymentFieldResolver databasePaymentFieldResolver;

    public FieldModel resolve(PaymentCheckedField field, PaymentModel model) {
        FieldModel fieldModel = databasePaymentFieldResolver.resolve(field, model);
        return new FieldModel(resolveName(field), fieldModel.getValue());
    }

    public String resolveName(PaymentCheckedField field) {
        return field.name();
    }

    @NotNull
    public List<FieldModel> resolveListFields(PaymentModel model, List<PaymentCheckedField> fields) {
        if (fields != null) {
            return fields.stream()
                    .map(field -> resolve(field, model))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
