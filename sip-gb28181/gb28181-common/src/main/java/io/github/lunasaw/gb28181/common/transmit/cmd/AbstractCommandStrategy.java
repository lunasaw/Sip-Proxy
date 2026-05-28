package io.github.lunasaw.gb28181.common.transmit.cmd;

import com.luna.common.check.Assert;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCommandStrategy implements CommandStrategy {

    @Override
    public final String execute(CommandContext ctx) {
        // 1.7.0：dialog-aware 命令（BYE / SUBSCRIBE_REFRESH）不再需要 from/to device
        // —— 信息全部从 dialog 取回（DialogRegistry 中的 callId）
        if (!isDialogAware(ctx)) {
            Assert.notNull(ctx.getFromDevice(), "fromDevice 不能为空");
            Assert.notNull(ctx.getToDevice(), "toDevice 不能为空");
        }
        validateContext(ctx);
        if (ctx.getContent() == null) {
            ctx.setContent(buildContent(ctx));
        }
        return doSend(ctx);
    }

    private static boolean isDialogAware(CommandContext ctx) {
        String type = ctx.getCommandType();
        return "BYE".equals(type) || "SUBSCRIBE_REFRESH".equals(type);
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
