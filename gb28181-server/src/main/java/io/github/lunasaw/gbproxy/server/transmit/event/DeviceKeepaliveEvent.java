package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import lombok.Getter;

@Getter
public class DeviceKeepaliveEvent extends DeviceEvent {

    private final DeviceKeepLiveNotify notify;

    public DeviceKeepaliveEvent(Object source, String deviceId, DeviceKeepLiveNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
