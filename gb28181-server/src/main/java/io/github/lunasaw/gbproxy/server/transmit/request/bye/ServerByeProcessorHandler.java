package io.github.lunasaw.gbproxy.server.transmit.request.bye;

import javax.sip.RequestEvent;

public interface ServerByeProcessorHandler {

    default boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        return true;
    }
}