package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestServerRegisterHandler implements DeviceLifecycleListener {

    private final SipBusinessConfig sessionCache;

    @Override
    public void onDeviceRegister(String deviceId, RegisterInfo info) {
        sessionCache.register(deviceId, info.getRemoteIp(), info.getRemotePort(), info.getTransport());
        log.info("设备注册信息更新: userId={}, ip={}:{}", deviceId, info.getRemoteIp(), info.getRemotePort());
    }

    @Override
    public void onDeviceOnline(String deviceId, SipTransaction sipTransaction) {
        log.info("设备上线: userId={}", deviceId);
    }

    @Override
    public void onDeviceOffline(String deviceId, RegisterInfo info, SipTransaction sipTransaction) {
        sessionCache.remove(deviceId);
        log.info("设备下线: userId={}", deviceId);
    }
}
