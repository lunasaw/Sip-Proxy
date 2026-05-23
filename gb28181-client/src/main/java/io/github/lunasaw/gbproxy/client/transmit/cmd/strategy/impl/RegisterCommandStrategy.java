package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

@Component("clientRegisterCommandStrategy")
public class RegisterCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "REGISTER"; }

    @Override
    protected String doSend(CommandContext ctx) {
        Integer expires = ctx.getExtra("expires", Integer.class);
        if (expires == null) expires = 3600;
        return SipSender.doRegisterRequest(ctx.getFromDevice(), ctx.getToDevice(), expires);
    }
}
