package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceInviteTryingEvent extends DeviceEvent {

    private final String callId;

    public DeviceInviteTryingEvent(Object source, String deviceId, String callId) {
        super(source, deviceId);
        this.callId = callId;
    }
}
