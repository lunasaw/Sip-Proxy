package io.github.lunasaw.gbproxy.server.transmit.event;

import org.springframework.context.ApplicationEvent;

public abstract class DeviceEvent extends ApplicationEvent {

    private final String deviceId;

    protected DeviceEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
