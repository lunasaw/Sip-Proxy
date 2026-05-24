package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台主动查询（rootType=Query）。
 *
 * <p>承载所有 query 类的多态 payload：
 * <ul>
 *   <li>cmdType=Catalog/DeviceInfo/DeviceStatus → {@code DeviceQuery}</li>
 *   <li>cmdType=RecordInfo → {@code DeviceRecordQuery}</li>
 *   <li>cmdType=Alarm → {@code DeviceAlarmQuery}</li>
 *   <li>cmdType=PTZPosition / SDCardStatus / HomePosition / CruiseTrack(List) → 对应 *Query</li>
 *   <li>cmdType=ConfigDownload → {@code DeviceConfigDownload} / {@code ConfigDownloadQuery}</li>
 *   <li>cmdType=Preset → {@code PresetQuery}</li>
 *   <li>cmdType=MobilePosition → {@code MobilePositionQuery}</li>
 * </ul>
 *
 * <p>Adapter 在 L2 用 {@code instanceof} + cmdType 字符串两阶分发到 {@code QueryListener}。
 * payload 父类是 {@code XmlBean} 而非 {@code DeviceBase} —— 12 个 query 类直接 extends XmlBean，
 * 7 个经过 DeviceBase，本事件持有最大公约数（XmlBean）。
 */
@Getter
public class ClientQueryEvent extends ApplicationEvent {

    private final String userId;
    private final String sipId;
    private final XmlBean query;

    public ClientQueryEvent(Object source, String userId, String sipId, XmlBean query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.query = query;
    }
}
