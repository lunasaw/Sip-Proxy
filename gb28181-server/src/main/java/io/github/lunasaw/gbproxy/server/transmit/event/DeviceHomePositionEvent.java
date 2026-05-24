package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import lombok.Getter;

/**
 * GB28181-2022 §9.5 / A.2.6.12 看守位信息查询应答事件
 *
 * @author luna
 */
@Getter
public class DeviceHomePositionEvent extends DeviceEvent {

    private final HomePositionResponse response;

    public DeviceHomePositionEvent(Object source, String deviceId, HomePositionResponse response) {
        super(source, deviceId);
        this.response = response;
    }
}
