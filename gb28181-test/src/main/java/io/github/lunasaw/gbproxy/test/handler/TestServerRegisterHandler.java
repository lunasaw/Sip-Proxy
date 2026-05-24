package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOfflineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOnlineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestServerRegisterHandler {

    private final SipBusinessConfig sessionCache;

    @EventListener
    public void onDeviceRegister(DeviceRegisterEvent event) {
        var info = event.getRegisterInfo();
        sessionCache.register(event.getDeviceId(), info.getRemoteIp(), info.getRemotePort(), info.getTransport());
        log.info("设备注册信息更新: userId={}, ip={}:{}", event.getDeviceId(), info.getRemoteIp(), info.getRemotePort());
    }

    @EventListener
    public void onDeviceOnline(DeviceOnlineEvent event) {
        log.info("设备上线: userId={}", event.getDeviceId());
    }

    @EventListener
    public void onDeviceOffline(DeviceOfflineEvent event) {
        sessionCache.remove(event.getDeviceId());
        log.info("设备下线: userId={}", event.getDeviceId());
    }
}
