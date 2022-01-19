package dev.vality.fraudbusters.serde;


import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.damsel.fraudbusters.Withdrawal;
import dev.vality.fraudbusters.config.service.ListenersConfigurationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WithdrawalDeserializer extends AbstractThriftDeserializer<Withdrawal> {

    @SneakyThrows
    @Override
    public Withdrawal deserialize(String topic, byte[] data) {
        try {
            return deserialize(data, new Withdrawal());
        } catch (Exception e) {
            log.warn("Error when WithdrawalDeserializer deserialize e: ", e);
            Thread.sleep(ListenersConfigurationService.THROTTLING_TIMEOUT);
            throw e;
        }
    }

}
