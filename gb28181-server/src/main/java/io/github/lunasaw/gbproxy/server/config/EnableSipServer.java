package io.github.lunasaw.gbproxy.server.config;

import io.github.lunasaw.gb28181.common.conf.Gb28181CommonAutoConfig;
import io.github.lunasaw.sip.common.conf.EnableSipProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 GB28181 平台服务端能力。
 * 自动激活 {@link SipProxyServerAutoConfig} 与 {@link io.github.lunasaw.sip.common.conf.SipProxyAutoConfig},
 * 业务方只需实现 {@link io.github.lunasaw.sip.common.service.ServerDeviceSupplier} 与
 * {@link io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache} 即可接入。
 *
 * @see EnableSipProxy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableSipProxy
@Import({Gb28181CommonAutoConfig.class, SipProxyServerAutoConfig.class})
public @interface EnableSipServer {
}
