package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.config.properties.DefaultTemplateProperties;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.fraudbusters.repository.PaymentRepository;
import dev.vality.fraudbusters.util.ReferenceKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopManagementService {

    private final PaymentRepository repository;
    private final DefaultTemplateProperties properties;
    private final Pool<String> referencePoolImpl;
    private final Pool<String> groupReferencePoolImpl;

    public boolean isNewShop(String partyId, String shopId) {
        if (hasReferenceInPools(partyId, shopId)) {
            return false;
        }
        Long to = Instant.now().toEpochMilli();
        Long from = Instant.now().minus(properties.getCountToCheckDays(), ChronoUnit.DAYS).toEpochMilli();
        return repository.countOperationByField("shopId", shopId, from, to) == 0;
    }

    private boolean hasReferenceInPools(String partyId, String shopId) {
        String partyShopKey = ReferenceKeyGenerator.generateTemplateKey(partyId, shopId);
        return referencePoolImpl.get(partyId) != null
                || referencePoolImpl.get(partyShopKey) != null
                || groupReferencePoolImpl.get(partyId) != null
                || groupReferencePoolImpl.get(partyShopKey) != null;
    }

}
