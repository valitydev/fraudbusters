package dev.vality.fraudbusters.listener.payment.historical;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.damsel.fraudbusters.Group;
import dev.vality.damsel.fraudbusters.PriorityId;
import dev.vality.fraudbusters.pool.HistoricalPool;
import dev.vality.fraudbusters.util.TimestampUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AbstractTimeGroupCommandListenerExecutor {

    protected void execCommand(Command command, HistoricalPool<List<String>> pool) {
        Group group = command.getCommandBody().getGroup();
        Long timestamp = TimestampUtil.parseInstantFromString(command.getCommandTime()).toEpochMilli();
        switch (command.command_type) {
            case CREATE -> createGroup(group, timestamp, pool);
            case DELETE -> pool.add(group.getGroupId(), timestamp, null);
            default -> log.error("Unknown command: {}", command);
        }
    }

    private void createGroup(Group group, Long time, HistoricalPool<List<String>> pool) {
        List<String> sortedListTemplates = group.template_ids.stream()
                .sorted(Comparator.comparing(PriorityId::getPriority))
                .map(PriorityId::getId)
                .collect(Collectors.toList());
        pool.add(group.getGroupId(), time, sortedListTemplates);
    }

}
