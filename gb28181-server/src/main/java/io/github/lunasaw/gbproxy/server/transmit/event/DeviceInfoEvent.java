package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import lombok.Getter;

@Getter
public class DeviceInfoEvent extends DeviceEvent {

    private final String sn;
    private final DeviceInfo info;

    public DeviceInfoEvent(Object source, String deviceId, String sn, DeviceInfo info) {
        super(source, deviceId);
        this.sn = sn;
        this.info = info;
    }
}
