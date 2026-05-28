package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.springframework.stereotype.Component;

/**
 * 服务端 INFO 命令策略，用于发送回放控制（暂停/恢复/定位/倍速）等 INFO 请求。
 */
@Component("serverInfoCommandStrategy")
public class InfoCommandStrategy extends AbstractServerCommandStrategy {

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
