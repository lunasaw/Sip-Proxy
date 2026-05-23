package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component
public class ByeCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "BYE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doByeRequest(ctx.getFromDevice(), ctx.getToDevice());
    }
}
