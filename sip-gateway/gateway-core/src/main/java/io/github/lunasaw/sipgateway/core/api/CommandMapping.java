package io.github.lunasaw.sipgateway.core.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令映射注解：标注在 Spring bean 的方法上，自动注册到 CommandHandlerRegistry。
 *
 * <p>方法签名约束：{@code (GatewayCommand) → String}（返回 correlationId）
 *
 * @author luna
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMapping {
    /**
     * 命令 type，如 "gb28181.Control.Ptz"。必须三段式。
     */
    String value();

    /**
     * 是否显式覆盖 ProtocolModule 注册的同 type 表条目。
     * 默认 false：覆盖时启动期仅 WARN，不阻断。
     * 建议覆盖时设为 true，作为意图声明。
     */
    boolean overrideTable() default false;
}
