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

    private final String userId;
    private final String sipId;
    private final Integer expires;
    private final XmlBean body;

    public ClientSubscribeEvent(Object source, String userId, String sipId, Integer expires, XmlBean body) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.expires = expires;
        this.body = body;
    }
}
