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

    private final String userId;
    private final XmlBean notify;

    public ClientNotifyEvent(Object source, String userId, XmlBean notify) {
        super(source);
        this.userId = userId;
        this.notify = notify;
    }
}
