package dev.vality.fraudbusters.serde;

import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.damsel.fraudbusters.FraudPayment;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FraudPaymentDeserializer extends AbstractThriftDeserializer<FraudPayment> {

    @SneakyThrows
    @Override
    public FraudPayment deserialize(String topic, byte[] data) {
        try {
            return deserialize(data, new FraudPayment());
        } catch (Exception e) {
            log.warn("Error when FraudPaymentDeserializer deserialize e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
