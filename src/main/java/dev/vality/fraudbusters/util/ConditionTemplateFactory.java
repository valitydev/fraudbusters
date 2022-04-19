package dev.vality.fraudbusters.util;

import dev.vality.fraudo.model.TrustCondition;
import dev.vality.fraudbusters.converter.TrustConditionToConditionConverter;
import dev.vality.trusted.tokens.ConditionTemplate;
import dev.vality.trusted.tokens.PaymentsConditions;
import dev.vality.trusted.tokens.WithdrawalsConditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ConditionTemplateFactory {

    private final TrustConditionToConditionConverter converter;

    public ConditionTemplate createConditionTemplate(List<TrustCondition> paymentsConditions,
                                                     List<TrustCondition> withdrawalsConditions) {
        ConditionTemplate conditionTemplate = new ConditionTemplate();
        if (!CollectionUtils.isEmpty(paymentsConditions)) {
            conditionTemplate.setPaymentsConditions(new PaymentsConditions()
                    .setConditions(converter.convertBatch(paymentsConditions)));
        }
        if (!CollectionUtils.isEmpty(withdrawalsConditions)) {
            conditionTemplate.setWithdrawalsConditions(new WithdrawalsConditions()
                    .setConditions(converter.convertBatch(withdrawalsConditions)));
        }
        return conditionTemplate;
    }

}
