package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import lombok.Getter;

/**
 * GB28181-2022 §9.5 / A.2.6.13 / A.2.6.14 巡航轨迹（列表 / 单条）查询应答事件。
 *
 * 通过 {@link Type} 区分轨迹列表与单条轨迹两种载荷。
 *
 * @author luna
 */
@Getter
public class DeviceCruiseTrackEvent extends DeviceEvent {

    public enum Type {
        LIST,
        SINGLE
    }

    private final Type type;
    private final CruiseTrackListResponse listResponse;
    private final CruiseTrackResponse trackResponse;

    public DeviceCruiseTrackEvent(Object source, String deviceId, CruiseTrackListResponse listResponse) {
        super(source, deviceId);
        this.type = Type.LIST;
        this.listResponse = listResponse;
        this.trackResponse = null;
    }

    public DeviceCruiseTrackEvent(Object source, String deviceId, CruiseTrackResponse trackResponse) {
        super(source, deviceId);
        this.type = Type.SINGLE;
        this.listResponse = null;
        this.trackResponse = trackResponse;
    }
}
