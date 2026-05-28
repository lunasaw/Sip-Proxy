package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台下发 ACK（确认 INVITE 200 OK）。
 */
@Getter
public class ClientAckEvent extends ApplicationEvent {

    /** 被确认会话的 Call-ID。 */
    private final String callId;

    /**
     * 构造 ACK 事件。
     *
     * @param source 事件来源对象
     * @param callId 被确认会话的 Call-ID
     */
    public ClientAckEvent(Object source, String callId) {
        super(source);
        this.callId = callId;
    }
}
