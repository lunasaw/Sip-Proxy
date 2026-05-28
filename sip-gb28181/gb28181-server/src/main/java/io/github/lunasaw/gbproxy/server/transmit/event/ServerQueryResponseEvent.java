package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Server 端 Layer 1 协议事件：设备应答（Catalog/Info/Status/Record/PTZ/SDCard/Home/Cruise/Config/...）。
 *
 * <p>承载多态 typed payload 与可选元数据（sn、callId、statusCode、reason 等），由 {@code ServerListenerAdapter}
 * 按 payload 类型分发到 {@code DeviceResponseListener} 的对应方法。
 *
 * @author luna
 */
@Getter
public class ServerQueryResponseEvent extends ApplicationEvent {

    private final String deviceId;
    private final String sn;
    /** typed payload：DeviceCatalog/DeviceInfo/DeviceStatus/DeviceRecord/PTZPositionResponse/SDCardStatusResponse/HomePositionResponse/CruiseTrackListResponse/CruiseTrackResponse/DeviceConfigResponse/DeviceOtherUpdateNotify */
    private final Object payload;
    /** 可选元数据：subscribe-response 携带 callId/statusCode；info-error 携带 reason；其它一般为 null */
    private final String extra;
    private final int extraCode;

    public ServerQueryResponseEvent(Object source, String deviceId, String sn, Object payload) {
        super(source);
        this.deviceId = deviceId;
        this.sn = sn;
        this.payload = payload;
        this.extra = null;
        this.extraCode = 0;
    }

    public ServerQueryResponseEvent(Object source, String deviceId, Object payload, String extra, int extraCode) {
        super(source);
        this.deviceId = deviceId;
        this.sn = null;
        this.payload = payload;
        this.extra = extra;
        this.extraCode = extraCode;
    }
}
