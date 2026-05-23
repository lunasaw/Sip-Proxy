package io.github.lunasaw.gbproxy.server.transmit.request.info;

import javax.sip.RequestEvent;

public interface ServerInfoProcessorHandler {

    default boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        return true;
    }
}