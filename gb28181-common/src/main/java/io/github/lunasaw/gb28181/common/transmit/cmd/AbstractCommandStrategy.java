package io.github.lunasaw.gb28181.common.transmit.cmd;

import com.luna.common.check.Assert;
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

    protected String buildContent(CommandContext ctx) {
        return ctx.getContent();
    }

    protected abstract String doSend(CommandContext ctx);
}
