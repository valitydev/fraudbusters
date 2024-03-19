package dev.vality.fraudbusters.listener.events.clickhouse;

import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.converter.FraudPaymentToRowConverter;
import dev.vality.fraudbusters.domain.FraudPaymentRow;
import dev.vality.fraudbusters.exception.UnknownFraudPaymentException;
import dev.vality.fraudbusters.repository.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudPaymentListener {

    private final Repository<FraudPaymentRow> repository;
    private final FraudPaymentToRowConverter fraudPaymentToRowConverter;

    @KafkaListener(topics = "${kafka.topic.fraud.payment}",
            containerFactory = "kafkaFraudPaymentListenerContainerFactory")
    public void listen(
            List<FraudPayment> payments, @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) throws InterruptedException {
        try {
            log.info("FraudPaymentListener listen result size: {} partition: {} offset: {} payments: {}",
                    payments.size(), partition, offset, payments
            );
            repository.insertBatch(payments.stream()
                    .map(fraudPaymentToRowConverter::convert)
                    .collect(Collectors.toList())
            );
        } catch (DateTimeParseException | UnknownFraudPaymentException e) {
            log.warn("Error when FraudPaymentListener listen e: ", e);
        } catch (Exception e) {
            log.warn("Error when FraudPaymentListener listen e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
