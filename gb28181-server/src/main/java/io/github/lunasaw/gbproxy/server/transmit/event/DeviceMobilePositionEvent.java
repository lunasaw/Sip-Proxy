package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import lombok.Getter;

@Getter
public class DeviceMobilePositionEvent extends DeviceEvent {

    private final MobilePositionNotify notify;

    public DeviceMobilePositionEvent(Object source, String deviceId, MobilePositionNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
