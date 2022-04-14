package dev.vality.fraudbusters.fraud.payment.resolver;

import dev.vality.fraudo.model.TrustCondition;
import dev.vality.fraudo.payment.resolver.CustomerTypeResolver;
import dev.vality.fraudbusters.exception.RuleFunctionException;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.pool.CardTokenPool;
import dev.vality.fraudbusters.util.ConditionTemplateFactory;
import dev.vality.trusted.tokens.TrustedTokensSrv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerTypeResolverImpl implements CustomerTypeResolver<PaymentModel> {

    private final CardTokenPool cardTokenPool;

    private final TrustedTokensSrv.Iface trustedTokensSrv;

    private final ConditionTemplateFactory conditionTemplateFactory;

    @Override
    public Boolean isTrusted(PaymentModel paymentModel) {
        return cardTokenPool.isExist(paymentModel.getCardToken());
    }

    @Override
    public Boolean isTrusted(PaymentModel model, String templateName) {
        try {
            return trustedTokensSrv.isTokenTrustedByConditionTemplateName(model.getCardToken(), templateName);
        } catch (TException e) {
            throw new RuleFunctionException(e);
        }
    }

    @Override
    public Boolean isTrusted(PaymentModel model, List<TrustCondition> paymentsConditions,
                             List<TrustCondition> withdrawalsConditions) {
        try {
            return trustedTokensSrv.isTokenTrusted(
                    model.getCardToken(),
                    conditionTemplateFactory.createConditionTemplate(paymentsConditions, withdrawalsConditions)
            );
        } catch (TException e) {
            throw new RuleFunctionException(e);
        }
    }

}
