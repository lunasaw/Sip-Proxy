package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

/**
 * 客户端 INVITE 命令发送策略，通过 {@link SipSender#doInviteRequest} 发起实时点播或历史回放。
 */
@Component("clientInviteCommandStrategy")
public class InviteCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "INVITE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doInviteRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
