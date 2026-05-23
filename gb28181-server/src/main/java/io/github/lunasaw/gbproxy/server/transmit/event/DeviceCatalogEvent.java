package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import lombok.Getter;

@Getter
public class DeviceCatalogEvent extends DeviceEvent {

    private final String sn;
    private final DeviceResponse catalog;

    public DeviceCatalogEvent(Object source, String deviceId, String sn, DeviceResponse catalog) {
        super(source, deviceId);
        this.sn = sn;
        this.catalog = catalog;
    }
}
