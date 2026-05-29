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

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 发起查询的上级平台 SIP 编码。 */
    private final String sipId;
    /** 查询请求体，具体类型由 cmdType 决定。 */
    private final XmlBean query;

    /**
     * 构造平台查询事件。
     *
     * @param source 事件来源对象
     * @param userId 本端设备编码
     * @param sipId  发起查询的上级平台 SIP 编码
     * @param query  查询请求体
     */
    public ClientQueryEvent(Object source, String userId, String sipId, XmlBean query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.query = query;
    }
}
