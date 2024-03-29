package dev.vality.fraudbusters.util;

import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.ConcreteResultModel;
import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.fraudo.model.ResultModel;
import dev.vality.fraudo.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CheckedResultFactory {

    @NonNull
    public Optional<CheckedResultModel> createCheckedResult(String templateKey, ResultModel resultModel) {
        return ResultUtils.findFirstNotNotifyStatus(resultModel).map(ruleResult -> {
            log.info("createCheckedResult resultModel: {}", resultModel);
            ConcreteResultModel concreteResultModel = new ConcreteResultModel();
            concreteResultModel.setResultStatus(ruleResult.getResultStatus());
            concreteResultModel.setRuleChecked(ruleResult.getRuleChecked());
            concreteResultModel.setNotificationsRule(ResultUtils.getNotifications(resultModel));

            CheckedResultModel checkedResultModel = new CheckedResultModel();
            checkedResultModel.setResultModel(concreteResultModel);
            checkedResultModel.setCheckedTemplate(templateKey);
            return checkedResultModel;
        });
    }

    @NonNull
    public CheckedResultModel createCheckedResultWithNotifications(String templateKey, ResultModel resultModel) {
        return createCheckedResult(templateKey, resultModel)
                .orElseGet(() ->
                        createNotificationOnlyResultModel(templateKey, ResultUtils.getNotifications(resultModel)));
    }

    @NonNull
    public CheckedResultModel createNotificationOnlyResultModel(String templateKey, List<String> notifications) {
        ConcreteResultModel concreteResultModel = new ConcreteResultModel();
        if (notifications != null && !notifications.isEmpty()) {
            concreteResultModel.setResultStatus(ResultStatus.NOTIFY);
        } else {
            concreteResultModel.setResultStatus(ResultStatus.NORMAL);
        }
        concreteResultModel.setNotificationsRule(notifications);
        CheckedResultModel checkedResultModel = new CheckedResultModel();
        checkedResultModel.setResultModel(concreteResultModel);
        checkedResultModel.setCheckedTemplate(templateKey);

        return checkedResultModel;
    }

}
