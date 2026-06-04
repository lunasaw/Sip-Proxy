package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.sip.common.constant.Constant;
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
 * <p>
 * {@link #useTransport(String)} 供 transport 矩阵测试在运行期切换 UDP/TCP —— 因为客户端收到
 * 401 后的认证重发 REGISTER 会从本 supplier 重新取 from/to, 必须让 supplier 整体产出同一 transport,
 * 否则第二程会退回默认 UDP。生产接入不需要此方法。
 */
@Slf4j
@Primary
@Component
public class TestClientDeviceSupplier implements ClientDeviceSupplier {

    private final String clientId;
    private final String clientIp;
    private final int    clientPort;
    private final String serverId;
    private final String serverIp;
    private final int    serverPort;
    private final String serverPassword;

    private String     transport = Constant.UDP;
    private FromDevice clientFromDevice;

    public TestClientDeviceSupplier(
            @Value("${sip.client.clientId}") String clientId,
            @Value("${sip.client.domain}") String clientIp,
            @Value("${sip.client.port}") int clientPort,
            @Value("${sip.server.serverId}") String serverId,
            @Value("${sip.server.ip}") String serverIp,
            @Value("${sip.server.port}") int serverPort,
            @Value("${sip.server.password}") String serverPassword) {
        this.clientId = clientId;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.serverId = serverId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.serverPassword = serverPassword;
        this.clientFromDevice = FromDevice.getInstance(clientId, clientIp, clientPort, transport);
    }

    /**
     * 切换本 supplier 产出设备的信令传输协议, 并重建 clientFromDevice。
     * 仅供 transport 矩阵测试使用。
     *
     * @param transport {@link Constant#UDP} 或 {@link Constant#TCP}
     */
    public void useTransport(String transport) {
        this.transport = transport;
        this.clientFromDevice = FromDevice.getInstance(clientId, clientIp, clientPort, transport);
    }

    @Override
    public Device getDevice(String userId) {
        if (serverId.equals(userId)) {
            ToDevice toDevice = ToDevice.getInstance(serverId, serverIp, serverPort, transport);
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
