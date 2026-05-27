package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台通知（rootType=Notify）。
 *
 * <p>客户端方向核心场景是语音广播 Broadcast（cmdType=Broadcast，payload=DeviceBroadcastNotify）。
 * 后续如新增其他 Notify 子类型，沿用此事件，Adapter 用 instanceof 多态分发。
 */
@Getter
public class ClientNotifyEvent extends ApplicationEvent {

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 通知消息体，具体类型由 cmdType 决定。 */
    private final XmlBean notify;

    /**
     * 构造平台通知事件。
     *
     * @param source 事件来源对象
     * @param userId 本端设备编码
     * @param notify 通知消息体
     */
    public ClientNotifyEvent(Object source, String userId, XmlBean notify) {
        super(source);
        this.userId = userId;
        this.notify = notify;
    }
}
