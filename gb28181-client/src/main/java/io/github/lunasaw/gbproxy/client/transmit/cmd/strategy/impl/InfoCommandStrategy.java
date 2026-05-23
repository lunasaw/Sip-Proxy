package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component("clientInfoCommandStrategy")
public class InfoCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "INFO"; }

    @Override
    protected String doSend(CommandContext ctx) {
        String controlBody = ctx.getExtra("controlBody", String.class);
        String content = controlBody != null ? controlBody : ctx.getContent();
        return SipSender.doInfoRequest(ctx.getFromDevice(), ctx.getToDevice(),
            content, ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
