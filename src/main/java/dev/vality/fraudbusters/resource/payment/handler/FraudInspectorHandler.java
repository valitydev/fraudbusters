package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.base.InvalidRequest;
import dev.vality.damsel.domain.RiskScore;
import dev.vality.damsel.proxy_inspector.*;
import dev.vality.damsel.wb_list.*;
import dev.vality.fraudbusters.converter.CheckedResultToRiskScoreConverter;
import dev.vality.fraudbusters.converter.ContextToFraudRequestConverter;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.domain.FraudRequest;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import dev.vality.fraudbusters.util.PaymentModelFactory;
import dev.vality.fraudbusters.util.UserCacheKeyUtil;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.CollectionUtils;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class FraudInspectorHandler implements InspectorProxySrv.Iface {

    private final String resultTopic;
    private final CheckedResultToRiskScoreConverter checkedResultToRiskScoreConverter;
    private final ContextToFraudRequestConverter requestConverter;
    private final TemplateVisitor<PaymentModel, CheckedResultModel> templateVisitor;
    private final KafkaTemplate<String, FraudResult> kafkaFraudResultTemplate;
    private final WbListServiceSrv.Iface wbListServiceSrv;

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

    @Override
    public boolean isBlacklisted(BlackListContext blackListContext) throws InvalidRequest, TException {
        try {
            Row row = new Row()
                    .setId(IdInfo.payment_id(new PaymentId()
                            .setPartyId(blackListContext.first_id)
                            .setShopId(blackListContext.second_id)))
                    .setListName(blackListContext.field_name)
                    .setListType(ListType.black)
                    .setValue(blackListContext.getValue());
            return wbListServiceSrv.isExist(row);
        } catch (Exception e) {
            log.warn("FraudInspectorHandler error when isExistInBlackList e: ", e);
            return false;
        }
    }

    @Override
    @Cacheable(
            cacheManager = "inspectUserCacheManager",
            cacheNames = "inspectUser",
            key = "#root.target.buildInspectUserCacheKey(#context)"
    )
    public BlockedShops inspectUser(InspectUserContext context) throws InvalidRequest, TException {
        if (CollectionUtils.isEmpty(context.getShopList())) {
            log.warn("FraudInspectorHandler inspectUser with empty shopList: {}", context);
            return new BlockedShops().setShopList(Collections.emptyList());
        }
        try {
            List<ShopContext> blockedShops = context.getShopList().stream()
                    .map(shopContext -> {
                        PaymentModel paymentModel = PaymentModelFactory.buildPaymentModel(context, shopContext);
                        CheckedResultModel result = templateVisitor.visit(paymentModel);
                        return new AbstractMap.SimpleEntry<>(shopContext, result);
                    })
                    .filter(entry -> isDeclineResult(entry.getValue()))
                    .map(AbstractMap.SimpleEntry::getKey)
                    .collect(Collectors.toList());
            log.debug("FraudInspectorHandler inspectUser result blockedShops: {}", blockedShops);
            return new BlockedShops().setShopList(blockedShops);
        } catch (Exception e) {
            log.warn("FraudInspectorHandler error when inspectUser e: ", e);
            return new BlockedShops().setShopList(Collections.emptyList());
        }
    }

    public String buildInspectUserCacheKey(InspectUserContext context) {
        return UserCacheKeyUtil.buildInspectUserCacheKey(context);
    }

    private static boolean isDeclineResult(CheckedResultModel result) {
        return result != null
               && result.getResultModel() != null
               && (ResultStatus.DECLINE.equals(result.getResultModel().getResultStatus())
                   || ResultStatus.DECLINE_AND_NOTIFY.equals(result.getResultModel().getResultStatus()));
    }

}
