package io.github.lunasaw.sipgateway.core.api;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;

/**
 * 命令处理器 SPI 接口（协议中立）。
 *
 * <p>每个 type 对应一个 CommandHandler 实例，由 CommandHandlerRegistry 管理。
 * 实现方式：① 表驱动（ReflectiveCommandHandler）② 注解方法（MethodInvokerHandler）
 *
 * @author luna
 */
public interface CommandHandler {
    String type();

    /**
     * 处理命令，返回 correlationId（sn 或 callId）。
     * sender 在实现侧持有，对外 SPI 不感知具体协议。
     */
    GatewayCommandResult handle(GatewayCommand cmd);
}
