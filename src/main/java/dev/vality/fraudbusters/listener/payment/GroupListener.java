package dev.vality.fraudbusters.listener.payment;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.fraudbusters.listener.AbstractGroupCommandListenerExecutor;
import dev.vality.fraudbusters.listener.CommandListener;
import dev.vality.fraudbusters.pool.Pool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class GroupListener extends AbstractGroupCommandListenerExecutor implements CommandListener {

    private final Pool<List<String>> groupPoolImpl;

    @Override
    @KafkaListener(topics = "${kafka.topic.group-list}", containerFactory = "groupListenerContainerFactory")
    public void listen(@Payload Command command) {
        log.info("GroupListener command: {}", command);
        if (command != null && command.isSetCommandBody() && command.getCommandBody().isSetGroup()) {
            execCommand(command, groupPoolImpl);
        }
    }

}
