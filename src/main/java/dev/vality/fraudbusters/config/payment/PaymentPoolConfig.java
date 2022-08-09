package dev.vality.fraudbusters.config.payment;

import dev.vality.fraudbusters.fraud.constant.PaymentCheckedField;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.fraudbusters.pool.PoolImpl;
import dev.vality.fraudbusters.stream.impl.RuleApplierImpl;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudo.payment.visitor.impl.FirstFindVisitorImpl;
import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PaymentPoolConfig {

    @Bean
    public Pool<List<String>> groupPoolImpl() {
        return new PoolImpl<>("group");
    }

    @Bean
    public Pool<String> referencePoolImpl() {
        return new PoolImpl<>("reference");
    }

    @Bean
    public Pool<String> groupReferencePoolImpl() {
        return new PoolImpl<>("group-reference");
    }

    @Bean
    public Pool<ParserRuleContext> templatePoolImpl() {
        return new PoolImpl<>("template");
    }

    @Bean
    public RuleApplierImpl<PaymentModel> ruleApplier(
            FirstFindVisitorImpl<PaymentModel, PaymentCheckedField> paymentRuleVisitor,
            Pool<ParserRuleContext> templatePoolImpl,
            CheckedResultFactory checkedResultFactory) {
        return new RuleApplierImpl<>(paymentRuleVisitor, templatePoolImpl, checkedResultFactory);
    }

}
