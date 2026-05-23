package io.github.lunasaw.sip.common.conf;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 SIP 代理框架基础协议层。
 * 标注在 {@link org.springframework.boot.autoconfigure.SpringBootApplication} 类上,
 * 激活 {@link SipProxyAutoConfig},完成 SipListener、请求/响应处理器的自动注册。
 * <p>
 * 业务方需自行实现 {@link io.github.lunasaw.sip.common.service.ClientDeviceSupplier}
 * 或 {@link io.github.lunasaw.sip.common.service.ServerDeviceSupplier} 提供设备信息。
 * <p>
 * 如需同时启用客户端 / 服务端业务能力, 推荐配合使用
 * {@code @EnableSipClient} / {@code @EnableSipServer}。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SipProxyAutoConfig.class)
public @interface EnableSipProxy {
}
