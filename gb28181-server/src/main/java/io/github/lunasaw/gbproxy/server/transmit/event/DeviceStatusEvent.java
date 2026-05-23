package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import lombok.Getter;

@Getter
public class DeviceStatusEvent extends DeviceEvent {

    private final String sn;
    private final DeviceStatus status;

    public DeviceStatusEvent(Object source, String deviceId, String sn, DeviceStatus status) {
        super(source, deviceId);
        this.sn = sn;
        this.status = status;
    }
}
