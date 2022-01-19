package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.service.dto.CascadingTemplateDto;

import java.util.Map;

public interface RuleCheckingService {

    Map<String, CheckedResultModel> checkSingleRule(Map<String, PaymentModel> paymentModelMap, String templateString);

    Map<String, CheckedResultModel> checkRuleWithinRuleset(
            Map<String, PaymentModel> paymentModelMap,
            CascadingTemplateDto cascadingTemplateDto
    );

}
