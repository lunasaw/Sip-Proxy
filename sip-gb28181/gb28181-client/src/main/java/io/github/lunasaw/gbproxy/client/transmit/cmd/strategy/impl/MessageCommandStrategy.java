package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

/**
 * 客户端 MESSAGE 命令发送策略，通过 {@link SipSender#doMessageRequest} 发送 GB28181 业务消息。
 */
@Component("clientMessageCommandStrategy")
public class MessageCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "MESSAGE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
