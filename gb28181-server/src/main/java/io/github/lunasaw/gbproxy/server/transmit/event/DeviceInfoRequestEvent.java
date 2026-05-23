package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceInfoRequestEvent extends DeviceEvent {

    private final String content;

    public DeviceInfoRequestEvent(Object source, String deviceId, String content) {
        super(source, deviceId);
        this.content = content;
    }
}
