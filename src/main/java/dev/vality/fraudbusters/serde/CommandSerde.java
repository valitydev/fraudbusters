package dev.vality.fraudbusters.serde;

import dev.vality.damsel.fraudbusters.Command;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@Slf4j
public class CommandSerde implements Serde<Command> {


    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public void close() {

    }

    @Override
    public Serializer<Command> serializer() {
        return new ThriftSerializer<>();
    }

    @Override
    public Deserializer<Command> deserializer() {
        return new CommandDeserializer();
    }
}
