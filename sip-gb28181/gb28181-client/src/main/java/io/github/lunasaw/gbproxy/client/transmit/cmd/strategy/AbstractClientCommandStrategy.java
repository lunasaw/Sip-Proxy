package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy;

import io.github.lunasaw.gb28181.common.transmit.cmd.AbstractCommandStrategy;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.utils.XmlUtils;

/**
 * 客户端侧命令策略抽象基类，固定 role 为 "client" 并提供通用的内容构建和发送逻辑。
 */
public abstract class AbstractClientCommandStrategy extends AbstractCommandStrategy {

    @Override
    public String getRole() {
        return "client";
    }

    @Override
    protected String buildContent(CommandContext ctx) {
        if (ctx.getContent() != null) return ctx.getContent();
        if (ctx.getBody() != null) return XmlUtils.toString("UTF-8", ctx.getBody());
        return null;
    }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
