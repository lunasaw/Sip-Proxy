package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

/**
 * 服务端 ACK 命令策略，支持带/不带 SDP body 的 ACK 发送。
 */
@Component("serverAckCommandStrategy")
public class AckCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() { return "ACK"; }

    @Override
    protected String doSend(CommandContext ctx) {
        String callId = ctx.getExtra("callId", String.class);
        if (ctx.getContent() != null) {
            return SipSender.doAckRequest(ctx.getFromDevice(), ctx.getToDevice(),
                ctx.getContent(), callId, ctx.getErrorEvent(), ctx.getOkEvent());
        }
        return SipSender.doAckRequest(ctx.getFromDevice(), ctx.getToDevice());
    }
}
