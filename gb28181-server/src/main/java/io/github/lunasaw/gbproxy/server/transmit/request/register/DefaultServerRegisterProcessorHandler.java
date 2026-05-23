package io.github.lunasaw.gbproxy.server.transmit.request.register;

import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "io.github.lunasaw.gbproxy.server.transmit.request.register.ServerRegisterProcessorHandler")
public class DefaultServerRegisterProcessorHandler implements ServerRegisterProcessorHandler {

    @Override
    public SipTransaction getDeviceTransaction(String userId) {
        return null;
    }

    @Override
    public boolean validatePassword(String userId, String password, RequestEvent evt) {
        return true;
    }
}
