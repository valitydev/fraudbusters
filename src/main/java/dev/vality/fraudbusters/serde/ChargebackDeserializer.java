package dev.vality.fraudbusters.serde;


import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.damsel.fraudbusters.Chargeback;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChargebackDeserializer extends AbstractThriftDeserializer<Chargeback> {

    @SneakyThrows
    @Override
    public Chargeback deserialize(String topic, byte[] data) {
        try {
            return deserialize(data, new Chargeback());
        } catch (Exception e) {
            log.warn("Error when ChargebackDeserializer deserialize e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
