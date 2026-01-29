package dev.vality.fraudbusters.resource.payment.handler;

import dev.vality.damsel.base.InvalidRequest;
import dev.vality.damsel.fraudbusters.BlockedShops;
import dev.vality.damsel.fraudbusters.InspectUserContext;
import dev.vality.damsel.fraudbusters.InspectorServiceSrv;
import dev.vality.damsel.fraudbusters.ShopContext;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.fraud.model.PaymentModel;
import dev.vality.fraudbusters.stream.TemplateVisitor;
import dev.vality.fraudbusters.util.PaymentModelFactory;
import dev.vality.fraudo.constant.ResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectorHandler implements InspectorServiceSrv.Iface {

    private final TemplateVisitor<PaymentModel, CheckedResultModel> templateVisitor;

    @Override
    @Cacheable(
            cacheManager = "inspectUserCacheManager",
            cacheNames = "inspectUser",
            key = "#root.target.buildInspectUserCacheKey(#context)"
    )
    public BlockedShops inspectUserShops(InspectUserContext context) throws InvalidRequest, TException {
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

    private static boolean isDeclineResult(CheckedResultModel result) {
        return result != null
               && result.getResultModel() != null
               && (ResultStatus.DECLINE.equals(result.getResultModel().getResultStatus())
                   || ResultStatus.DECLINE_AND_NOTIFY.equals(result.getResultModel().getResultStatus()));
    }


}
