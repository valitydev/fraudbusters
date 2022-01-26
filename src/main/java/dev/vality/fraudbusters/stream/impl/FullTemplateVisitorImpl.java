package dev.vality.fraudbusters.stream.impl;

import dev.vality.fraudbusters.constant.TemplateLevel;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.ConcreteResultModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import dev.vality.fraudbusters.util.ReferenceKeyGenerator;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FullTemplateVisitorImpl implements TemplateVisitor<PaymentModel, List<CheckedResultModel>> {

    private static final String RULE_NOT_CHECKED = "RULE_NOT_CHECKED";

    private final FullRuleApplierImpl fullRuleApplier;
    private final HistoricalPool<List<String>> timeGroupPoolImpl;
    private final HistoricalPool<String> timeReferencePoolImpl;
    private final HistoricalPool<String> timeGroupReferencePoolImpl;

    @Override
    public List<CheckedResultModel> visit(PaymentModel paymentModel) {
        log.debug("FullTemplateVisitorImpl visit paymentModel: {}", paymentModel);
        String partyId = paymentModel.getPartyId();
        Long timestamp = initTimestamp(paymentModel);
        List<CheckedResultModel> checkedResultModels = new ArrayList<>();
        String partyShopKey = ReferenceKeyGenerator.generateTemplateKeyByList(partyId, paymentModel.getShopId());
        fullRuleApplier.apply(paymentModel, timeReferencePoolImpl.get(TemplateLevel.GLOBAL.name(), timestamp))
                .ifPresent(checkedResultModels::add);
        applyGroupIfContain(paymentModel, timestamp, checkedResultModels, partyId);
        applyGroupIfContain(paymentModel, timestamp, checkedResultModels, partyShopKey);
        applyReferenceIfContain(paymentModel, partyId, timestamp, checkedResultModels);
        applyReferenceIfContain(paymentModel, partyShopKey, timestamp, checkedResultModels);
        if (checkedResultModels.isEmpty()) {
            checkedResultModels.add(createDefaultResult());
        }
        log.debug("FullTemplateVisitorImpl visit checkedResultModels: {}", checkedResultModels);
        return checkedResultModels;
    }

    private void applyReferenceIfContain(
            PaymentModel paymentModel,
            String partyId,
            Long timestamp,
            List<CheckedResultModel> checkedResultModels) {
        if (timeReferencePoolImpl.contains(partyId, timestamp)) {
            fullRuleApplier.apply(paymentModel, timeReferencePoolImpl.get(partyId, timestamp))
                    .ifPresent(checkedResultModels::add);
        }
    }

    private void applyGroupIfContain(
            PaymentModel paymentModel,
            Long timestamp,
            List<CheckedResultModel> checkedResultModels,
            String partyShopKey) {
        if (timeGroupReferencePoolImpl.contains(partyShopKey, timestamp)
                && timeGroupPoolImpl.contains(timeGroupReferencePoolImpl.get(partyShopKey, timestamp), timestamp)) {
            fullRuleApplier
                    .applyForAny(
                            paymentModel,
                            timeGroupPoolImpl.get(timeGroupReferencePoolImpl.get(partyShopKey, timestamp), timestamp)
                    )
                    .ifPresent(checkedResultModels::add);
        }
    }

    private long initTimestamp(PaymentModel paymentModel) {
        return paymentModel.getTimestamp() != null ? paymentModel.getTimestamp() : Instant.now().toEpochMilli();
    }

    @NotNull
    private CheckedResultModel createDefaultResult() {
        ConcreteResultModel resultModel = new ConcreteResultModel();
        resultModel.setResultStatus(ResultStatus.THREE_DS);
        CheckedResultModel checkedResultModel = new CheckedResultModel();
        checkedResultModel.setResultModel(resultModel);
        checkedResultModel.setCheckedTemplate(RULE_NOT_CHECKED);
        return checkedResultModel;
    }

}
