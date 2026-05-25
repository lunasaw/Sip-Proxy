package io.github.lunasaw.gb28181.common.transmit.cmd;

import com.luna.common.check.Assert;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCommandStrategy implements CommandStrategy {

    @Override
    public final String execute(CommandContext ctx) {
        Assert.notNull(ctx.getFromDevice(), "fromDevice 不能为空");
        Assert.notNull(ctx.getToDevice(), "toDevice 不能为空");
        validateContext(ctx);
        if (ctx.getContent() == null) {
            ctx.setContent(buildContent(ctx));
        }
        return doSend(ctx);
    }

    protected void validateContext(CommandContext ctx) {}

    /**
     * 默认：把 {@code body} JAXB 序列化为 XML。子类只在需要非 XML 体（SDP / MANSRTSP）时重写。
     * MESSAGE/NOTIFY/SUBSCRIBE 都是 GB28181 XML 体，沿用默认即可。
     */
    protected String buildContent(CommandContext ctx) {
        if (ctx.getContent() != null) {
            return ctx.getContent();
        }
        Object body = ctx.getBody();
        if (body == null) {
            return null;
        }
        if (body instanceof String s) {
            return s;
        }
        return XmlUtils.toString("UTF-8", body);
    }

    protected abstract String doSend(CommandContext ctx);
}
