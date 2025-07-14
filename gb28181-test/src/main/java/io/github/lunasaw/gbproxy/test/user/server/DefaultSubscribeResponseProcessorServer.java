package io.github.lunasaw.gbproxy.test.user.server;

import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.server.transimit.response.subscribe.SubscribeResponseProcessorServer;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2023/12/25
 */
@Component
@Slf4j
public class DefaultSubscribeResponseProcessorServer implements SubscribeResponseProcessorServer {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Override
    public void responseSubscribe(DeviceSubscribe deviceSubscribe) {
        Device fromDevice = sipUserGenerateServer.getFromDevice();
        log.info("responseSubscribe::userId = {}, fromDevice = {}", deviceSubscribe.getDeviceId(), fromDevice);

    }
}
