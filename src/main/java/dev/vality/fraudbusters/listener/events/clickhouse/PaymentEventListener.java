package dev.vality.fraudbusters.listener.events.clickhouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.fraudbusters.converter.PaymentToCheckedPaymentConverter;
import dev.vality.fraudbusters.converter.PaymentToPaymentModelConverter;
import dev.vality.fraudbusters.domain.CheckedPayment;
import dev.vality.fraudbusters.domain.CheckedResultModel;
import dev.vality.fraudbusters.fraud.localstorage.LocalResultStorage;
import dev.vality.fraudbusters.repository.Repository;
import dev.vality.fraudbusters.stream.impl.FullTemplateVisitorImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final Repository<CheckedPayment> repository;
    private final FullTemplateVisitorImpl fullTemplateVisitor;
    private final PaymentToPaymentModelConverter paymentToPaymentModelConverter;
    private final PaymentToCheckedPaymentConverter paymentToCheckedPaymentConverter;

    private final LocalResultStorage localResultStorage;
    private final ObjectMapper objectMapper;

    @Value("${result.full.check.enabled:true}")
    private boolean isEnabledFullCheck;

    @KafkaListener(topics = "${kafka.topic.event.sink.payment}",
            containerFactory = "kafkaPaymentResultListenerContainerFactory")
    public void listen(
            List<Payment> payments,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) throws InterruptedException {
        try {
            log.info(
                    "PaymentEventListener listen result size: {} partition: {} offset: {}",
                    payments.size(),
                    partition,
                    offset
            );
            log.debug("PaymentEventListener listen result payments: {}", payments);
            repository.insertBatch(
                    payments.stream()
                            .map(this::mapAndCheckResults)
                            .collect(Collectors.toList())
            );
            localResultStorage.clear();
        } catch (Exception e) {
            log.warn("Error when PaymentEventListener listen e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

    private CheckedPayment mapAndCheckResults(Payment payment) {
        CheckedPayment checkedPayment = paymentToCheckedPaymentConverter.convert(payment);
        if (isEnabledFullCheck && PaymentStatus.processed.name().equals(checkedPayment.getPaymentStatus())) {
            List<CheckedResultModel> listResults =
                    fullTemplateVisitor.visit(paymentToPaymentModelConverter.convert(payment));
            Optional<CheckedResultModel> first = listResults.stream()
                    .filter(checkedResultModel -> checkedResultModel.getCheckedTemplate() != null)
                    .findFirst();
            if (first.isPresent()) {
                CheckedResultModel checkedResultModel = first.get();
                checkedPayment.setCheckedTemplate(checkedResultModel.getCheckedTemplate());
                checkedPayment.setResultStatus(checkedResultModel.getResultModel().getResultStatus().name());
                checkedPayment.setCheckedRule(checkedResultModel.getResultModel().getRuleChecked());
                try {
                    checkedPayment.setCheckedResultsJson(objectMapper.writeValueAsString(listResults));
                } catch (JsonProcessingException e) {
                    log.warn("PaymentEventListener problem with serialize json! listResults: {}", listResults);
                }
            }
        }
        localResultStorage.get().add(checkedPayment);
        return checkedPayment;
    }
}
