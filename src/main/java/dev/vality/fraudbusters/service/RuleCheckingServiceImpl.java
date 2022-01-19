package dev.vality.fraudbusters.service;

import com.rbkmoney.fraudo.FraudoPaymentParser;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.exception.InvalidTemplateException;
import dev.vality.fraudbusters.fraud.FraudContextParser;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.fraud.payment.validator.PaymentTemplateValidator;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.service.dto.CascadingTemplateDto;
import dev.vality.fraudbusters.stream.impl.RuleCheckingApplierImpl;
import dev.vality.fraudbusters.util.CheckedResultFactory;
import dev.vality.fraudbusters.util.CheckedResultModelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.vality.fraudbusters.util.ReferenceKeyGenerator.generateTemplateKey;

@Slf4j
@RequiredArgsConstructor
@Service
public class RuleCheckingServiceImpl implements RuleCheckingService {

    private final PaymentTemplateValidator paymentTemplateValidator;
    private final FraudContextParser<FraudoPaymentParser.ParseContext> paymentContextParser;
    private final RuleCheckingApplierImpl<PaymentModel> ruleCheckingApplier;
    private final HistoricalPool<List<String>> timeGroupPoolImpl;
    private final HistoricalPool<String> timeReferencePoolImpl;
    private final HistoricalPool<String> timeGroupReferencePoolImpl;
    private final CheckedResultFactory checkedResultFactory;

    @Override
    public Map<String, CheckedResultModel> checkSingleRule(Map<String, PaymentModel> paymentModelMap,
                                                           String templateString) {
        validateTemplate(templateString);
        final FraudoPaymentParser.ParseContext parseContext = paymentContextParser.parse(templateString);
        return paymentModelMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> ruleCheckingApplier
                                .applyWithContext(entry.getValue(), templateString, parseContext)
                                .orElseGet(() -> checkedResultFactory.createNotificationOnlyResultModel(
                                        templateString, null))
                ));
    }

    @Override
    public Map<String, CheckedResultModel> checkRuleWithinRuleset(Map<String, PaymentModel> paymentModelMap,
                                                                  CascadingTemplateDto cascadingTemplateDto) {
        validateTemplate(cascadingTemplateDto.getTemplate());
        return paymentModelMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> checkWithinRuleset(entry.getValue(), cascadingTemplateDto)
                ));
    }


    private CheckedResultModel checkWithinRuleset(PaymentModel paymentModel, CascadingTemplateDto dto) {
        log.debug("HistoricalTemplateVisitorImpl visit paymentModel: {}", paymentModel);
        final FraudoPaymentParser.ParseContext parseContext = paymentContextParser.parse(dto.getTemplate());
        Long timestamp = dto.getTimestamp() == null ? paymentModel.getTimestamp() : dto.getTimestamp();
        String partyId = paymentModel.getPartyId();
        String partyShopKey = generateTemplateKey(partyId, paymentModel.getShopId());
        List<String> notifications = new ArrayList<>();
        return applyGroupTemplateByAttribute(paymentModel, timestamp, notifications, partyId)
                .orElseGet(() -> applyGroupTemplateByAttribute(paymentModel, timestamp, notifications, partyShopKey)
                        .orElseGet(() -> applyTemplateByPartyId(paymentModel, timestamp, notifications, partyId,
                                dto, parseContext)
                                .orElseGet(() -> applyTemplateByPartyShopKey(paymentModel, timestamp, notifications,
                                        partyShopKey, dto, parseContext)
                                        .orElseGet(() -> checkedResultFactory.createNotificationOnlyResultModel(
                                                dto.getTemplate(), notifications)
                                        ))));
    }

    private Optional<CheckedResultModel> applyGroupTemplateByAttribute(PaymentModel paymentModel,
                                                                       Long timestamp,
                                                                       List<String> notifications,
                                                                       String referenceAttribute) {
        Optional<CheckedResultModel> result = ruleCheckingApplier.applyForAny(
                paymentModel,
                timeGroupPoolImpl.get(timeGroupReferencePoolImpl.get(referenceAttribute, timestamp), timestamp),
                timestamp
        );
        return processRuleCheckingApplierResult(result, notifications);
    }

    private Optional<CheckedResultModel> applyTemplateByPartyId(PaymentModel paymentModel,
                                                                Long timestamp,
                                                                List<String> notifications,
                                                                String partyId,
                                                                CascadingTemplateDto dto,
                                                                FraudoPaymentParser.ParseContext parseContext) {
        if (isSubstituteOnPartyLevel(partyId, dto)) {
            return applyExactRule(paymentModel, dto.getTemplate(), parseContext, notifications);
        }
        return applyTemplateByAttribute(paymentModel, timestamp, notifications, partyId);
    }

    private Optional<CheckedResultModel> applyTemplateByPartyShopKey(PaymentModel paymentModel,
                                                                     Long timestamp,
                                                                     List<String> notifications,
                                                                     String partyShopKey,
                                                                     CascadingTemplateDto dto,
                                                                     FraudoPaymentParser.ParseContext parseContext) {
        if (isSubstituteOnPartyShopLevel(partyShopKey, dto)) {
            return applyExactRule(paymentModel, dto.getTemplate(), parseContext, notifications);
        }
        return applyTemplateByAttribute(paymentModel, timestamp, notifications, partyShopKey);
    }

    private Optional<CheckedResultModel> applyTemplateByAttribute(PaymentModel paymentModel,
                                                                  Long timestamp,
                                                                  List<String> notifications,
                                                                  String referenceAttribute) {
        Optional<CheckedResultModel> result = ruleCheckingApplier.apply(
                paymentModel,
                timeReferencePoolImpl.get(referenceAttribute, timestamp),
                timestamp
        );
        return processRuleCheckingApplierResult(result, notifications);
    }

    private Optional<CheckedResultModel> applyExactRule(PaymentModel paymentModel,
                                                        String templateString,
                                                        FraudoPaymentParser.ParseContext parseContext,
                                                        List<String> notifications) {
        Optional<CheckedResultModel> result =
                ruleCheckingApplier.applyWithContext(paymentModel, templateString, parseContext);
        return processRuleCheckingApplierResult(result, notifications);
    }


    private boolean isSubstituteOnPartyLevel(String modelPartyId, CascadingTemplateDto dto) {
        return dto.getShopId() == null && modelPartyId.equals(dto.getPartyId());
    }

    private boolean isSubstituteOnPartyShopLevel(String modelPartyShopKey, CascadingTemplateDto dto) {
        return dto.getShopId() != null
                && modelPartyShopKey.equals(generateTemplateKey(dto.getPartyId(), dto.getShopId()));
    }

    private Optional<CheckedResultModel> processRuleCheckingApplierResult(Optional<CheckedResultModel> optional,
                                                                          List<String> notifications) {
        if (optional.isPresent()) {
            CheckedResultModel model = optional.get();
            if (CheckedResultModelUtil.isTerminal(model)) {
                CheckedResultModelUtil.finalizeCheckedResultModel(model, notifications);
                return Optional.of(model);
            } else {
                notifications.addAll(CheckedResultModelUtil.extractNotifications(optional));
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void validateTemplate(String templateString) {
        List<String> validationErrors = paymentTemplateValidator.validate(templateString);
        if (!CollectionUtils.isEmpty(validationErrors)) {
            throw new InvalidTemplateException(templateString, validationErrors);
        }
    }
}
