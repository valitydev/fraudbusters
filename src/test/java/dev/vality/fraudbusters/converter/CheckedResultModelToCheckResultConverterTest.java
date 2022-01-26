package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.Accept;
import dev.vality.damsel.fraudbusters.CheckResult;
import dev.vality.damsel.fraudbusters.ConcreteCheckResult;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.ConcreteResultModel;
import dev.vality.fraudo.constant.ResultStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckedResultModelToCheckResultConverterTest {

    private final CheckedResultModelToCheckResultConverter converter =
            new CheckedResultModelToCheckResultConverter(new ResultStatusConverter());

    @Test
    void convert() {
        final String template = UUID.randomUUID().toString();
        final String ruleChecked = UUID.randomUUID().toString();
        final String firstNotification = UUID.randomUUID().toString();
        final String secondNotification = UUID.randomUUID().toString();

        ConcreteResultModel concreteResultModel = new ConcreteResultModel();
        concreteResultModel.setRuleChecked(ruleChecked);
        concreteResultModel.setResultStatus(ResultStatus.ACCEPT);
        concreteResultModel.setNotificationsRule(List.of(firstNotification, secondNotification));
        CheckedResultModel input = new CheckedResultModel();
        input.setCheckedTemplate(template);
        input.setResultModel(concreteResultModel);

        var accept = new dev.vality.damsel.fraudbusters.ResultStatus();
        accept.setAccept(new Accept());
        CheckResult expected = new CheckResult()
                .setCheckedTemplate(template)
                .setConcreteCheckResult(new ConcreteCheckResult()
                        .setResultStatus(accept)
                        .setRuleChecked(ruleChecked)
                        .setNotificationsRule(List.of(firstNotification, secondNotification))
                );

        assertEquals(expected, converter.convert(input));
    }
}
