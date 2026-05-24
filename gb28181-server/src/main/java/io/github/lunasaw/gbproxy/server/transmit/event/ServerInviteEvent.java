package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 服务端收到设备主动发起的 INVITE（如语音对讲场景）。
 *
 * <p>处理器侧已立即回 100 Trying 并将 RequestEvent 存入
 * {@link io.github.lunasaw.sip.common.transmit.SipTransactionRegistry}，
 * 业务方监听此事件后可异步准备 SDP，再通过 {@code transactionContextKey}
 * 取回 RequestEvent 调用 {@code ResponseCmd.sendResponse(200, sdp, evt)} 完成回包。
 *
 * <p>UDP 重传场景下此事件可能被多次发布，业务方需按 callId 自行幂等。
 */
@Getter
public class ServerInviteEvent extends ApplicationEvent {

    private final String callId;
    private final String fromUserId;
    private final String toUserId;
    private final GbSessionDescription sessionDescription;
    private final String transactionContextKey;

    public ServerInviteEvent(Object source, String callId, String fromUserId, String toUserId,
                             GbSessionDescription sessionDescription, String transactionContextKey) {
        super(source);
        this.callId = callId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.sessionDescription = sessionDescription;
        this.transactionContextKey = transactionContextKey;
    }
}
