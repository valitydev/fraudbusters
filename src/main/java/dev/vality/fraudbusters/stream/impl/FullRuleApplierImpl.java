package dev.vality.fraudbusters.stream.impl;

import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.stream.RuleApplier;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudo.model.ResultModel;
import dev.vality.fraudo.visitor.TemplateVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class FullRuleApplierImpl implements RuleApplier<PaymentModel> {

    private final TemplateVisitor<PaymentModel, ResultModel> templateVisitor;

    private final HistoricalPool<ParserRuleContext> templatePool;
    private final CheckedResultFactory checkedResultFactory;

    @Override
    public Optional<CheckedResultModel> apply(PaymentModel model, String templateKey) {
        ParserRuleContext parseContext = templatePool.get(templateKey, model.getTimestamp());
        if (parseContext != null) {
            ResultModel resultModel = templateVisitor.visit(parseContext, model);
            return checkedResultFactory.createCheckedResult(templateKey, resultModel);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CheckedResultModel> applyForAny(PaymentModel model, List<String> templateKeys) {
        if (templateKeys != null) {
            for (String templateKey : templateKeys) {
                Optional<CheckedResultModel> result = apply(model, templateKey);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

}
