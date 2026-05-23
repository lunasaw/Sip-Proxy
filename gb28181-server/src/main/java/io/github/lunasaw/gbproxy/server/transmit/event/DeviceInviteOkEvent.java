package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceInviteOkEvent extends DeviceEvent {

    private final String callId;

    public DeviceInviteOkEvent(Object source, String deviceId, String callId) {
        super(source, deviceId);
        this.callId = callId;
    }
}
