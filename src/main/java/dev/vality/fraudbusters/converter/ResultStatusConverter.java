package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.*;
import dev.vality.fraudo.constant.ResultStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResultStatusConverter implements Converter<ResultStatus, dev.vality.damsel.fraudbusters.ResultStatus> {

    private static final String UNKNOWN_VALUE = "Unknown ResultStatus";

    @Override
    public dev.vality.damsel.fraudbusters.ResultStatus convert(ResultStatus resultStatus) {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        switch (resultStatus) {
            case TRUST -> status.setTrust(new Trust());
            case ACCEPT -> status.setAccept(new Accept());
            case ACCEPT_AND_NOTIFY -> status.setAcceptAndNotify(new AcceptAndNotify());
            case THREE_DS -> status.setThreeDs(new ThreeDs());
            case DECLINE -> status.setDecline(new Decline());
            case DECLINE_AND_NOTIFY -> status.setDeclineAndNotify(new DeclineAndNotify());
            case HIGH_RISK -> status.setHighRisk(new HighRisk());
            case NORMAL -> status.setNormal(new Normal());
            case NOTIFY -> status.setNotify(new Notify());
            default -> throw new IllegalArgumentException(UNKNOWN_VALUE);
        }
        return status;
    }

}
