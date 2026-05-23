package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component
public class MessageCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() { return "MESSAGE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
