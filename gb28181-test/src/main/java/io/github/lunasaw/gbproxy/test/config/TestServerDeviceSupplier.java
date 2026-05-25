package io.github.lunasaw.gbproxy.test.config;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.gbproxy.server.transmit.request.register.DigestServerAuthenticationHelper;
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
 *
 * <p>业务方接入要点:
 * <ul>
 *   <li>{@link #getServerFromDevice()} 返回本地平台身份, 用于 SIP 消息的 From</li>
 *   <li>{@link #getDevice(String)} 返回已注册设备的寻址信息, 委托给业务方维护的
 *       {@link DeviceSessionCache} (本工程为内存实现 {@link SipBusinessConfig})</li>
 *   <li>{@link #authenticate(String, SIPRequest)} 实现 HTTP Digest 鉴权,
 *       生产场景应根据 userId 取出对应明文密码, 测试场景统一使用 {@code sip.server.password}</li>
 * </ul>
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

    @Value("${sip.server.password}")
    private String serverPassword;

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

    @Override
    public boolean authenticate(String userId, SIPRequest request) {
        return DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, serverPassword);
    }
}
