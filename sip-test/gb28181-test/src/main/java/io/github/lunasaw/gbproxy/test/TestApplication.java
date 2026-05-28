package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.config.EnableSipClient;
import io.github.lunasaw.gbproxy.server.config.EnableSipServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GB28181 测试 / 业务接入示范入口。
 * <p>
 * 业务方接入步骤:
 * <ol>
 *   <li>在 {@code @SpringBootApplication} 类上叠加 {@link EnableSipServer} 或 {@link EnableSipClient}
 *       (本工程同时演示客户端 + 服务端,所以两个都加)</li>
 *   <li>实现 {@link io.github.lunasaw.sip.common.service.ServerDeviceSupplier} /
 *       {@link io.github.lunasaw.sip.common.service.ClientDeviceSupplier} 提供本地设备身份</li>
 *   <li>实现 {@link io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache} 提供已注册设备寻址</li>
 *   <li>用 {@code @EventListener} 监听 {@code DeviceXxxEvent} / {@code ClientXxxEvent} 处理业务回调</li>
 * </ol>
 */
@SpringBootApplication
@EnableSipServer
@EnableSipClient
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
