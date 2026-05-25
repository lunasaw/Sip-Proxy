package io.github.lunasaw.gb28181.common.transmit.cmd;

public interface CommandStrategy {
    String execute(CommandContext ctx);
    String getCommandType();
    String getRole();
}
