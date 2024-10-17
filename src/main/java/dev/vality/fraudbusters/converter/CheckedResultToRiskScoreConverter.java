package dev.vality.fraudbusters.converter;

import dev.vality.damsel.domain.RiskScore;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CheckedResultToRiskScoreConverter implements Converter<CheckedResultModel, RiskScore> {

    @Override
    public RiskScore convert(CheckedResultModel checkedResultModel) {
        return switch (checkedResultModel.getResultModel().getResultStatus()) {
            case TRUST -> RiskScore.trusted;
            case ACCEPT, ACCEPT_AND_NOTIFY, NOTIFY -> RiskScore.low;
            case DECLINE, DECLINE_AND_NOTIFY -> RiskScore.fatal;
            default -> RiskScore.high;
        };
    }

}
