package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 测试 / 业务接入示范用 ServerDeviceSupplier。
 * <p>
 * 业务方接入要点:
 * <ul>
 *   <li>{@link #getServerFromDevice()} 返回本地平台身份, 用于 SIP 消息的 From</li>
 *   <li>{@link #getDevice(String)} 返回已注册设备的寻址信息, 委托给业务方维护的
 *       {@link DeviceSessionCache} (本工程为内存实现 {@link InMemoryDeviceSessionCache})</li>
 * </ul>
 * 用 {@code @Primary} 覆盖框架默认的 {@code DefaultServerDeviceSupplier}。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class TestServerDeviceSupplier implements ServerDeviceSupplier {

    private final DeviceSessionCache sessionCache;

    @Value("${sip.server.serverId}")
    private String serverId;

    @Value("${sip.server.ip}")
    private String serverIp;

    @Value("${sip.server.port}")
    private int serverPort;

    private FromDevice serverFromDevice;

    @Override
    public Device getDevice(String userId) {
        return sessionCache.getToDevice(userId);
    }

    @Override
    public FromDevice getServerFromDevice() {
        if (serverFromDevice == null) {
            serverFromDevice = FromDevice.getInstance(serverId, serverIp, serverPort);
        }
        return serverFromDevice;
    }

    @Override
    public void setServerFromDevice(FromDevice fromDevice) {
        this.serverFromDevice = fromDevice;
    }

    @Override
    public ToDevice getToDevice(String deviceId) {
        return sessionCache.getToDevice(deviceId);
    }
}
