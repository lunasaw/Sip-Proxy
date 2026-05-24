package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import lombok.Getter;

/**
 * GB28181-2022 §9.5 / A.2.6.15 PTZ 精确状态查询应答事件
 *
 * @author luna
 */
@Getter
public class DevicePtzPositionEvent extends DeviceEvent {

    private final PTZPositionResponse response;

    public DevicePtzPositionEvent(Object source, String deviceId, PTZPositionResponse response) {
        super(source, deviceId);
        this.response = response;
    }
}
