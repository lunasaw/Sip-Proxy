package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component("serverRegisterCommandStrategy")
public class RegisterCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() { return "REGISTER"; }

    @Override
    protected String doSend(CommandContext ctx) {
        Integer expires = ctx.getExtra("expires", Integer.class);
        if (expires == null) expires = 3600;
        return SipSender.doRegisterRequest(ctx.getFromDevice(), ctx.getToDevice(),
            expires, ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
