package dev.vality.fraudbusters.serde;

import dev.vality.damsel.fraudbusters.Refund;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RefundDeserializer extends AbstractThriftDeserializer<Refund> {

    @SneakyThrows
    @Override
    public Refund deserialize(String topic, byte[] data) {
        try {
            return deserialize(data, new Refund());
        } catch (Exception e) {
            log.warn("Error when RefundDeserializer deserialize e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
