package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * REGISTER 200 OK 事件。
 *
 * <p>v1.7.x 起携带对端 GBT-28181 协议版本（{@link #peerProtocolVersion}），
 * 缺失时为 {@code null}（对端为 2016 之前实现，未携带 X-GB-Ver 扩展头）。
 */
@Getter
public class ClientRegisterSuccessEvent extends ApplicationEvent {

    private final String userId;
    /**
     * 平台 X-GB-Ver 头域值（GBT-28181-2022 附录 I）。null 表示对端未携带此扩展头。
     */
    private final String peerProtocolVersion;

    public ClientRegisterSuccessEvent(Object source, String userId) {
        this(source, userId, null);
    }

    public ClientRegisterSuccessEvent(Object source, String userId, String peerProtocolVersion) {
        super(source);
        this.userId = userId;
        this.peerProtocolVersion = peerProtocolVersion;
    }
}
