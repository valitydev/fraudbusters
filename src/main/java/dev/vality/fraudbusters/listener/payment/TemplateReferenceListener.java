package dev.vality.fraudbusters.listener.payment;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.damsel.fraudbusters.TemplateReference;
import dev.vality.fraudbusters.listener.AbstractPoolCommandListenerExecutor;
import dev.vality.fraudbusters.listener.CommandListener;
import dev.vality.fraudbusters.pool.Pool;
import dev.vality.fraudbusters.util.ReferenceKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateReferenceListener extends AbstractPoolCommandListenerExecutor implements CommandListener {

    private final Pool<String> referencePoolImpl;

    @Override
    @KafkaListener(topics = "${kafka.topic.reference}", containerFactory = "referenceListenerContainerFactory")
    public void listen(@Payload Command command) {
        log.info("TemplateReferenceListener command: {}", command);
        if (command != null && command.isSetCommandBody() && command.getCommandBody().isSetReference()) {
            TemplateReference reference = command.getCommandBody().getReference();
            String key = ReferenceKeyGenerator.generateTemplateKey(reference);
            TemplateReference templateReference = command.getCommandBody().getReference();
            execCommand(command, key, referencePoolImpl, templateReference::getTemplateId);
        }
    }

}
