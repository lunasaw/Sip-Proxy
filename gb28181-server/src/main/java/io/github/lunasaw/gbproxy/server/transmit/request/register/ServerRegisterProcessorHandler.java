package io.github.lunasaw.gbproxy.server.transmit.request.register;

import io.github.lunasaw.sip.common.entity.SipTransaction;

import javax.sip.RequestEvent;

public interface ServerRegisterProcessorHandler {

    default boolean validatePassword(String userId, String password, RequestEvent evt) {
        return true;
    }

    default SipTransaction getDeviceTransaction(String userId) {
        return null;
    }

    default Integer getDeviceExpire(String userId) {
        return 3600;
    }
}