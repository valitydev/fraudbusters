package dev.vality.fraudbusters.listener.payment;

import dev.vality.damsel.fraudbusters.MerchantInfo;
import dev.vality.damsel.fraudbusters.ReferenceInfo;
import dev.vality.fraudbusters.config.properties.DefaultTemplateProperties;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.converter.FraudResultToEventConverter;
import dev.vality.fraudbusters.domain.Event;
import dev.vality.fraudbusters.domain.FraudResult;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.service.InitiatingEntitySourceService;
import dev.vality.fraudbusters.service.ShopManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultAggregatorListener {

    private final Repository<Event> repository;
    private final FraudResultToEventConverter fraudResultToEventConverter;
    private final ShopManagementService shopManagementService;
    private final DefaultTemplateProperties defaultTemplateProperties;
    private final InitiatingEntitySourceService initiatingEntitySourceService;

    @KafkaListener(topics = "${kafka.topic.result}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(
            List<FraudResult> batch, @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) throws InterruptedException {
        try {
            log.info("ResultAggregatorListener listen result size: {} partition: {} offset: {}",
                    batch.size(), partition, offset
            );
            List<Event> events = fraudResultToEventConverter.convertBatch(batch);
            if (defaultTemplateProperties.isEnable()) {
                events.stream()
                        .filter(event -> shopManagementService.isNewShop(event.getPartyId(), event.getShopId()))
                        .forEach(event -> initiatingEntitySourceService.sendToSource(ReferenceInfo.merchant_info(
                                new MerchantInfo()
                                        .setShopId(event.getShopId())
                                        .setPartyId(event.getPartyId())))
                        );
            }
            repository.insertBatch(events);
        } catch (Exception e) {
            log.warn("Error when ResultAggregatorListener listen e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }
}
