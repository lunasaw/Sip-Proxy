package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台下发 INVITE（实时点播 / 历史回放）。
 */
@Getter
public class ClientInviteEvent extends ApplicationEvent {

    /** 本次会话的 Call-ID。 */
    private final String callId;
    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** SDP 会话描述，包含媒体流参数。 */
    private final SdpSessionDescription sessionDescription;
    /** 事务上下文键，用于异步响应路由。 */
    private final String transactionContextKey;

    /**
     * 构造 INVITE 事件。
     *
     * @param source                事件来源对象
     * @param callId                本次会话的 Call-ID
     * @param userId                本端设备编码
     * @param sessionDescription    SDP 会话描述
     * @param transactionContextKey 事务上下文键
     */
    public ClientInviteEvent(Object source, String callId, String userId, SdpSessionDescription sessionDescription, String transactionContextKey) {
        super(source);
        this.callId = callId;
        this.userId = userId;
        this.sessionDescription = sessionDescription;
        this.transactionContextKey = transactionContextKey;
    }
}
