package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import lombok.Getter;

@Getter
public class DeviceConfigEvent extends DeviceEvent {

    private final String sn;
    private final DeviceConfigResponse config;

    public DeviceConfigEvent(Object source, String deviceId, String sn, DeviceConfigResponse config) {
        super(source, deviceId);
        this.sn = sn;
        this.config = config;
    }
}
