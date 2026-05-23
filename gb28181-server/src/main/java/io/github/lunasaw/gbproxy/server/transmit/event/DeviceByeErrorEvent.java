package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceByeErrorEvent extends DeviceEvent {

    private final String errorMessage;

    public DeviceByeErrorEvent(Object source, String deviceId, String errorMessage) {
        super(source, deviceId);
        this.errorMessage = errorMessage;
    }
}
