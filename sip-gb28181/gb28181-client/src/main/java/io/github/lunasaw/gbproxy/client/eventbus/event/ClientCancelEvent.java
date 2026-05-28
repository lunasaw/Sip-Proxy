package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台下发 CANCEL（取消 INVITE）。
 */
@Getter
public class ClientCancelEvent extends ApplicationEvent {

    /** 被取消会话的 Call-ID。 */
    private final String callId;
    /** SIP 响应状态码。 */
    private final int statusCode;

    /**
     * 构造 CANCEL 事件。
     *
     * @param source     事件来源对象
     * @param callId     被取消会话的 Call-ID
     * @param statusCode SIP 响应状态码
     */
    public ClientCancelEvent(Object source, String callId, int statusCode) {
        super(source);
        this.callId = callId;
        this.statusCode = statusCode;
    }
}
