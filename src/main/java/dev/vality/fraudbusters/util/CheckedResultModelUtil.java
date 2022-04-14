package dev.vality.fraudbusters.util;

import dev.vality.fraudo.constant.ResultStatus;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CheckedResultModelUtil {

    private static final Set<ResultStatus> NON_TERMINAL_STATUSES = EnumSet.of(ResultStatus.NORMAL, ResultStatus.NOTIFY);

    public static CheckedResultModel finalizeCheckedResultModel(CheckedResultModel model, List<String> notifications) {
        if (notifications != null && !notifications.isEmpty()) {
            if (model.getResultModel().getNotificationsRule() == null) {
                model.getResultModel().setNotificationsRule(notifications);
            } else {
                model.getResultModel().getNotificationsRule().addAll(notifications);
            }
        }
        return model;
    }

    public static boolean isTerminal(CheckedResultModel model) {
        return model != null
                && model.getResultModel() != null
                && model.getResultModel().getResultStatus() != null
                && !NON_TERMINAL_STATUSES.contains(model.getResultModel().getResultStatus());
    }

    public static List<String> extractNotifications(Optional<CheckedResultModel> optionalCheckedResult) {
        return optionalCheckedResult
                .filter(model -> model.getResultModel() != null
                        && model.getResultModel().getNotificationsRule() != null)
                .map(model -> model.getResultModel().getNotificationsRule())
                .orElseGet(ArrayList::new);
    }

}
