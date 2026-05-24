package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import lombok.Getter;

/**
 * GB28181-2022 §9.5 / A.2.6.16 存储卡状态查询应答事件
 *
 * @author luna
 */
@Getter
public class DeviceSdCardStatusEvent extends DeviceEvent {

    private final SDCardStatusResponse response;

    public DeviceSdCardStatusEvent(Object source, String deviceId, SDCardStatusResponse response) {
        super(source, deviceId);
        this.response = response;
    }
}
