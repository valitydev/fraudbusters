package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.CheckResult;
import dev.vality.damsel.fraudbusters.ConcreteCheckResult;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CheckedResultModelToCheckResultConverter implements Converter<CheckedResultModel, CheckResult> {

    private final ResultStatusConverter resultStatusConverter;

    @Override
    public CheckResult convert(CheckedResultModel model) {
        return new CheckResult()
                .setCheckedTemplate(model.getCheckedTemplate())
                .setConcreteCheckResult(new ConcreteCheckResult()
                        .setResultStatus(resultStatusConverter.convert(model.getResultModel().getResultStatus()))
                        .setRuleChecked(model.getResultModel().getRuleChecked())
                        .setNotificationsRule(model.getResultModel().getNotificationsRule()));
    }

}
