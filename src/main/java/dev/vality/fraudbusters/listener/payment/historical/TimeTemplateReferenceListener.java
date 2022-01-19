package dev.vality.fraudbusters.listener.payment.historical;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.damsel.fraudbusters.TemplateReference;
import dev.vality.fraudbusters.listener.CommandListener;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.util.ReferenceKeyGenerator;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeTemplateReferenceListener extends AbstractTimePoolCommandListenerExecutor implements CommandListener {

    private final HistoricalPool<String> timeReferencePoolImpl;

    @Override
    @KafkaListener(autoStartup = "${kafka.historical.listener.enable}", topics = "${kafka.topic.full-reference}",
            containerFactory = "timeReferenceListenerContainerFactory")
    public void listen(@Payload Command command) {
        log.info("TimeTemplateReferenceListener command: {}", command);
        if (command != null && command.isSetCommandBody() && command.getCommandBody().isSetReference()) {
            TemplateReference reference = command.getCommandBody().getReference();
            String key = ReferenceKeyGenerator.generateTemplateKey(reference);
            TemplateReference templateReference = command.getCommandBody().getReference();
            Long timestamp = TimestampUtil.parseInstantFromString(command.getCommandTime()).toEpochMilli();
            execCommand(command, key, timestamp, timeReferencePoolImpl, templateReference::getTemplateId);
        }
    }

}
