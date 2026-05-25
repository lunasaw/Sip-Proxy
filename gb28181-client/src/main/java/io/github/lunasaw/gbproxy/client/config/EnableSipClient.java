package io.github.lunasaw.gbproxy.client.config;

import io.github.lunasaw.gb28181.common.conf.Gb28181CommonAutoConfig;
import io.github.lunasaw.sip.common.conf.EnableSipProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 GB28181 设备客户端能力。
 * 自动激活 {@link SipProxyClientAutoConfig} 与 {@link io.github.lunasaw.sip.common.conf.SipProxyAutoConfig},
 * 业务方只需实现 {@link io.github.lunasaw.sip.common.service.ClientDeviceSupplier} 提供本地设备信息。
 *
 * @see EnableSipProxy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableSipProxy
@Import({Gb28181CommonAutoConfig.class, SipProxyClientAutoConfig.class})
public @interface EnableSipClient {
}
