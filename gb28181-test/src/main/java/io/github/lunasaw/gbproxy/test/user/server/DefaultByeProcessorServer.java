package io.github.lunasaw.gbproxy.test.user.server;

import io.github.lunasaw.gbproxy.server.transimit.request.bye.ByeProcessorServer;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @date 2023/12/25
 */
@Component
@Slf4j
public class DefaultByeProcessorServer implements ByeProcessorServer {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Override
    public void receiveBye(String userId) {
        Device fromDevice = sipUserGenerateServer.getFromDevice();
        log.info("receiveBye::userId = {}, fromDevice = {}", userId, fromDevice);
    }
}
