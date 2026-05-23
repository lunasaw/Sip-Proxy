package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import lombok.Getter;

@Getter
public class DeviceNotifyUpdateEvent extends DeviceEvent {

    private final DeviceOtherUpdateNotify notify;

    public DeviceNotifyUpdateEvent(Object source, String deviceId, DeviceOtherUpdateNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
