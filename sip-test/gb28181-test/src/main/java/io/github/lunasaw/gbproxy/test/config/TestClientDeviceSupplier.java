package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 测试 / 业务接入示范用 ClientDeviceSupplier。
 * <p>
 * 业务方接入要点:
 * <ul>
 *   <li>{@link #getClientFromDevice()} 返回本地客户端身份, 用于 SIP 消息的 From</li>
 *   <li>{@link #getDevice(String)} 返回目标设备 (通常是平台/服务端) 信息, 含 password,
 *       供 Digest 认证时使用</li>
 * </ul>
 * 用 {@code @Primary} 覆盖框架默认的 {@code DefaultClientDeviceSupplier}, 业务方在自己的工程里
 * 同样以这种方式接入即可。
 */
@Slf4j
@Primary
@Component
public class TestClientDeviceSupplier implements ClientDeviceSupplier {

    private final String serverId;
    private final String serverIp;
    private final int    serverPort;
    private final String serverPassword;

    private FromDevice clientFromDevice;

    public TestClientDeviceSupplier(
            @Value("${sip.client.clientId}") String clientId,
            @Value("${sip.client.domain}") String clientIp,
            @Value("${sip.client.port}") int clientPort,
            @Value("${sip.server.serverId}") String serverId,
            @Value("${sip.server.ip}") String serverIp,
            @Value("${sip.server.port}") int serverPort,
            @Value("${sip.server.password}") String serverPassword) {
        this.serverId = serverId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.serverPassword = serverPassword;
        this.clientFromDevice = FromDevice.getInstance(clientId, clientIp, clientPort);
    }

    @Override
    public Device getDevice(String userId) {
        if (serverId.equals(userId)) {
            ToDevice toDevice = ToDevice.getInstance(serverId, serverIp, serverPort);
            toDevice.setPassword(serverPassword);
            return toDevice;
        }
        return null;
    }

    @Override
    public FromDevice getClientFromDevice() {
        return clientFromDevice;
    }

    @Override
    public void setClientFromDevice(FromDevice fromDevice) {
        this.clientFromDevice = fromDevice;
    }
}
