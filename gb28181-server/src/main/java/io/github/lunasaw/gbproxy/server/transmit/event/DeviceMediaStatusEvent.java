package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import lombok.Getter;

@Getter
public class DeviceMediaStatusEvent extends DeviceEvent {

    private final MediaStatusNotify notify;

    public DeviceMediaStatusEvent(Object source, String deviceId, MediaStatusNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
