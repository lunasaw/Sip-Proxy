package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceByeEvent extends DeviceEvent {

    public DeviceByeEvent(Object source, String deviceId) {
        super(source, deviceId);
    }
}
