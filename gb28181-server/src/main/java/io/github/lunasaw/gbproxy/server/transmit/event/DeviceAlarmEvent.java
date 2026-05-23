package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import lombok.Getter;

@Getter
public class DeviceAlarmEvent extends DeviceEvent {

    private final DeviceAlarmNotify notify;

    public DeviceAlarmEvent(Object source, String deviceId, DeviceAlarmNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
