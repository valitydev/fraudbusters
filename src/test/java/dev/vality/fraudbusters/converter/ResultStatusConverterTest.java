package dev.vality.fraudbusters.converter;

import com.rbkmoney.fraudo.constant.ResultStatus;
import dev.vality.damsel.fraudbusters.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultStatusConverterTest {

    private final ResultStatusConverter converter = new ResultStatusConverter();

    @Test
    void convertAccept() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setAccept(new Accept());
        assertEquals(status, converter.convert(ResultStatus.ACCEPT));
    }

    @Test
    void convertAcceptAndNotify() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setAcceptAndNotify(new AcceptAndNotify());
        assertEquals(status, converter.convert(ResultStatus.ACCEPT_AND_NOTIFY));
    }

    @Test
    void convertThreeDs() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setThreeDs(new ThreeDs());
        assertEquals(status, converter.convert(ResultStatus.THREE_DS));
    }

    @Test
    void convertDecline() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setDecline(new Decline());
        assertEquals(status, converter.convert(ResultStatus.DECLINE));
    }

    @Test
    void convertDeclineAndNotify() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setDeclineAndNotify(new DeclineAndNotify());
        assertEquals(status, converter.convert(ResultStatus.DECLINE_AND_NOTIFY));
    }

    @Test
    void convertHighRisk() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setHighRisk(new HighRisk());
        assertEquals(status, converter.convert(ResultStatus.HIGH_RISK));
    }

    @Test
    void convertNormal() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setNormal(new Normal());
        assertEquals(status, converter.convert(ResultStatus.NORMAL));
    }

    @Test
    void convertNotify() {
        var status = new dev.vality.damsel.fraudbusters.ResultStatus();
        status.setNotify(new Notify());
        assertEquals(status, converter.convert(ResultStatus.NOTIFY));
    }
}
