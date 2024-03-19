package dev.vality.fraudbusters.listener.events.clickhouse;

import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.repository.Repository;
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
public class RefundEventListener {

    private final Repository<Refund> repository;

    @KafkaListener(topics = "${kafka.topic.event.sink.refund}",
            containerFactory = "kafkaRefundResultListenerContainerFactory")
    public void listen(
            List<Refund> refunds,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) throws InterruptedException {
        try {
            log.info(
                    "RefundEventListener listen result size: {} partition: {} offset: {}",
                    refunds.size(),
                    partition,
                    offset
            );
            repository.insertBatch(refunds);
        } catch (Exception e) {
            log.warn("Error when RefundEventListener listen e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }
}
