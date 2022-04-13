package dev.vality.fraudbusters.domain;

import dev.vality.fraudo.constant.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcreteResultModel {

    private ResultStatus resultStatus;
    private String ruleChecked;
    private List<String> notificationsRule;

}
