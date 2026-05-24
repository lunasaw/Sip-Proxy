package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.KeepaliveControl;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台 Keepalive（rootType=Control，cmdType=Keepalive）。
 *
 * <p>独立于 {@link ClientControlEvent} 而非合并 —— 真正的拆分理由是 cmdType 不同（L0 由独立
 * {@code KeepaliveMessageClientHandler} 接收）+ 语义不同（心跳是状态上报，不是控制指令）。
 */
@Getter
public class ClientKeepaliveEvent extends ApplicationEvent {

    private final String userId;
    private final KeepaliveControl keepalive;

    public ClientKeepaliveEvent(Object source, String userId, KeepaliveControl keepalive) {
        super(source);
        this.userId = userId;
        this.keepalive = keepalive;
    }
}
