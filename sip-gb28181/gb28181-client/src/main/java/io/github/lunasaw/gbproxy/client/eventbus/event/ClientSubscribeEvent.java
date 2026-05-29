package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台订阅（method=SUBSCRIBE）。
 *
 * <p>fire-and-forget 语义：协议层 200 OK 由 L0 handler 同步返回（毫秒级，
 * 不依赖 listener），本事件仅作"已接受订阅"通知。listener 不能拒绝订阅。
 *
 * <p>payload 多态：
 * <ul>
 *   <li>cmdType=Catalog（订阅）→ {@code DeviceQuery}</li>
 *   <li>cmdType=Alarm（订阅）→ {@code DeviceAlarmQuery}</li>
 *   <li>cmdType=MobilePosition（订阅）→ {@code DeviceMobileQuery}</li>
 * </ul>
 */
@Getter
public class ClientSubscribeEvent extends ApplicationEvent {

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 发起订阅的上级平台 SIP 编码。 */
    private final String sipId;
    /** 订阅有效期（秒），由 SIP Expires 头携带。 */
    private final Integer expires;
    /** 订阅请求体，具体类型由 cmdType 决定。 */
    private final XmlBean body;

    /**
     * 构造平台订阅事件。
     *
     * @param source  事件来源对象
     * @param userId  本端设备编码
     * @param sipId   发起订阅的上级平台 SIP 编码
     * @param expires 订阅有效期（秒）
     * @param body    订阅请求体
     */
    public ClientSubscribeEvent(Object source, String userId, String sipId, Integer expires, XmlBean body) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.expires = expires;
        this.body = body;
    }
}
