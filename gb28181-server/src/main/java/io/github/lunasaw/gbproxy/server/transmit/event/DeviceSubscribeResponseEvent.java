package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceSubscribeResponseEvent extends DeviceEvent {

    private final String callId;
    private final int statusCode;

    public DeviceSubscribeResponseEvent(Object source, String deviceId, String callId, int statusCode) {
        super(source, deviceId);
        this.callId = callId;
        this.statusCode = statusCode;
    }
}
