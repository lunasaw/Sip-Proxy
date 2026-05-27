package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

/**
 * 服务端 MESSAGE 命令策略，用于发送 GB28181 查询/控制/配置类 MESSAGE 请求。
 */
@Component("serverMessageCommandStrategy")
public class MessageCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() { return "MESSAGE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
