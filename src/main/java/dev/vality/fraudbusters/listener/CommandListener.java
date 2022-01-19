package dev.vality.fraudbusters.listener;

import dev.vality.damsel.fraudbusters.Command;

public interface CommandListener {

    void listen(Command command);

}
