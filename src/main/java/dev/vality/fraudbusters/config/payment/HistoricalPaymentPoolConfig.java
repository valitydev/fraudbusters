package dev.vality.fraudbusters.config.payment;

import com.rbkmoney.fraudo.payment.visitor.impl.FirstFindVisitorImpl;
import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.pool.HistoricalPoolImpl;
import dev.vality.fraudbusters.stream.impl.FullRuleApplierImpl;
import dev.vality.fraudbusters.stream.impl.RuleCheckingApplierImpl;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class HistoricalPaymentPoolConfig {

    @Bean
    public HistoricalPool<List<String>> timeGroupPoolImpl() {
        return new HistoricalPoolImpl<>("time-group-pool");
    }

    @Bean
    public HistoricalPool<String> timeReferencePoolImpl() {
        return new HistoricalPoolImpl<>("time-reference-pool");
    }

    @Bean
    public HistoricalPool<String> timeGroupReferencePoolImpl() {
        return new HistoricalPoolImpl<>("time-group-reference-pool");
    }

    @Bean
    public HistoricalPool<ParserRuleContext> timeTemplatePoolImpl() {
        return new HistoricalPoolImpl<>("time-template-pool");
    }

    @Bean
    public FullRuleApplierImpl fullRuleApplier(
            FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> fullPaymentRuleVisitor,
            HistoricalPool<ParserRuleContext> templatePoolImpl,
            CheckedResultFactory checkedResultFactory) {
        return new FullRuleApplierImpl(fullPaymentRuleVisitor, templatePoolImpl, checkedResultFactory);
    }

    @Bean
    public RuleCheckingApplierImpl<PaymentModel> ruleCheckingApplier(
            FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> paymentRuleVisitor,
            HistoricalPool<ParserRuleContext> timeTemplatePoolImpl,
            CheckedResultFactory checkedResultFactory) {
        return new RuleCheckingApplierImpl<>(paymentRuleVisitor, timeTemplatePoolImpl, checkedResultFactory);
    }

}
