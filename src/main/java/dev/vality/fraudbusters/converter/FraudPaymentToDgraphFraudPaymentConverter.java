package dev.vality.fraudbusters.converter;

import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.fraudbusters.domain.dgraph.common.DgraphFraudPayment;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FraudPaymentToDgraphFraudPaymentConverter implements Converter<FraudPayment, DgraphFraudPayment>  {

    @Override
    public DgraphFraudPayment convert(FraudPayment fraudPayment) {
        DgraphFraudPayment dgraphFraudPayment = new DgraphFraudPayment();
        dgraphFraudPayment.setPaymentId(fraudPayment.getId());
        dgraphFraudPayment.setCreatedAt(TimestampUtil.parseDate(fraudPayment.getEventTime()).toString());
        dgraphFraudPayment.setFraudType(fraudPayment.getType());
        dgraphFraudPayment.setComment(fraudPayment.getComment());
        return dgraphFraudPayment;
    }

}
