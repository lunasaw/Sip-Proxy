package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOfflineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOnlineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.register.DigestServerAuthenticationHelper;
import io.github.lunasaw.gbproxy.server.transmit.request.register.ServerRegisterProcessorHandler;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class TestServerRegisterHandler implements ServerRegisterProcessorHandler {

    private final SipBusinessConfig sessionCache;

    @Value("${sip.server.password:12345678}")
    private String serverPassword;

    @Override
    public boolean validatePassword(String userId, String password, RequestEvent evt) {
        return DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(evt.getRequest(), serverPassword);
    }

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
