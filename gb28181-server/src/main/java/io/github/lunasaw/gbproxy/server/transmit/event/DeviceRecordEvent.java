package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import lombok.Getter;

@Getter
public class DeviceRecordEvent extends DeviceEvent {

    private final String sn;
    private final DeviceRecord record;

    public DeviceRecordEvent(Object source, String deviceId, String sn, DeviceRecord record) {
        super(source, deviceId);
        this.sn = sn;
        this.record = record;
    }
}
