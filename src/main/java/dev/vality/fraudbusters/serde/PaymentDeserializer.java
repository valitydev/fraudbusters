package dev.vality.fraudbusters.serde;


import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentDeserializer extends AbstractThriftDeserializer<Payment> {

    @SneakyThrows
    @Override
    public Payment deserialize(String topic, byte[] data) {
        try {
            return deserialize(data, new Payment());
        } catch (Exception e) {
            log.warn("Error when PaymentDeserializer deserialize e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
