package io.github.lunasaw.gbproxy.test.user.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.server.transimit.request.info.InfoProcessorServer;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2023/12/25
 */
@Component
@Slf4j
public class DefaultInfoProcessorServer implements InfoProcessorServer {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Override
    public void dealInfo(String userId, String content) {
        Device fromDevice = sipUserGenerateServer.getFromDevice();
        log.info("deviceInfo::userId = {}, fromDevice = {}, xml = {}", userId, fromDevice, content);

    }
}
