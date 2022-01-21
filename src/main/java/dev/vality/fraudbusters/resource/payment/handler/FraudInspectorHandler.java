package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.domain.RiskScore;
import dev.vality.damsel.proxy_inspector.Context;
import dev.vality.damsel.proxy_inspector.InspectorProxySrv;
import dev.vality.fraudbusters.converter.CheckedResultToRiskScoreConverter;
import dev.vality.fraudbusters.converter.ContextToFraudRequestConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.FraudRequest;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class FraudInspectorHandler implements InspectorProxySrv.Iface {

    private final String resultTopic;
    private final CheckedResultToRiskScoreConverter checkedResultToRiskScoreConverter;
    private final ContextToFraudRequestConverter requestConverter;
    private final TemplateVisitor<PaymentModel, CheckedResultModel> templateVisitor;
    private final KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate;

    @Override
    public RiskScore inspectPayment(Context context) throws TException {
        try {
            FraudRequest model = requestConverter.convert(context);
            if (model != null) {
                log.info("Check fraudRequest: {}", model);
                FraudResult fraudResult = new FraudResult(model, templateVisitor.visit(model.getFraudModel()));
                kafkaFraudResultTemplate.send(resultTopic, fraudResult);
                log.info("Checked fraudResult: {}", fraudResult);
                return checkedResultToRiskScoreConverter.convert(fraudResult.getResultModel());
            }
            return RiskScore.high;
        } catch (Exception e) {
            log.error("Error when inspectPayment() e: ", e);
            throw new TException("Error when inspectPayment() e: ", e);
        }
    }


}
