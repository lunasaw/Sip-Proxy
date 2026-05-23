package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component
public class AckCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "ACK"; }

    @Override
    protected String doSend(CommandContext ctx) {
        String callId = ctx.getExtra("callId", String.class);
        return SipSender.doAckRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), callId, ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
