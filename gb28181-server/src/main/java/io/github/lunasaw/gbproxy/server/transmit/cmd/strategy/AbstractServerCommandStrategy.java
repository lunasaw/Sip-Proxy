package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy;

import io.github.lunasaw.gb28181.common.transmit.cmd.AbstractCommandStrategy;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.sip.common.transmit.SipSender;

/**
 * 服务端出向命令策略抽象基类，固定 role 为 "server"，默认使用 MESSAGE 发送方式。
 */
public abstract class AbstractServerCommandStrategy extends AbstractCommandStrategy {

    @Override
    public String getRole() {
        return "server";
    }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
