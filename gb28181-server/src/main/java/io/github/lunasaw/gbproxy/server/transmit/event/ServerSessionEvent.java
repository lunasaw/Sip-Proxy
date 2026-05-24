package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.api.dto.SessionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Server 端 Layer 1 协议事件：INVITE/BYE/ACK 状态机。
 *
 * <p>用 {@link SessionType} 区分子语义（trying/ok/failure/ack/bye/byeError/serverInvite），
 * 由 {@code ServerListenerAdapter} 分发到 {@code DeviceSessionListener} 的对应方法。
 *
 * @author luna
 */
@Getter
public class ServerSessionEvent extends ApplicationEvent {

    private final SessionType type;
    /** 设备 ID（serverInvite 场景下为 toUserId） */
    private final String deviceId;
    private final String callId;
    /** invite-failure / ack 携带 SIP 状态码 */
    private final int statusCode;
    /** byeError 携带错误描述 */
    private final String errorMessage;
    /** serverInvite 场景下：fromUserId / toUserId / SDP / transactionContextKey */
    private final String fromUserId;
    private final String toUserId;
    private final GbSessionDescription sessionDescription;
    private final String transactionContextKey;

    private ServerSessionEvent(Object source, SessionType type, String deviceId, String callId, int statusCode,
                               String errorMessage, String fromUserId, String toUserId,
                               GbSessionDescription sd, String transactionContextKey) {
        super(source);
        this.type = type;
        this.deviceId = deviceId;
        this.callId = callId;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.sessionDescription = sd;
        this.transactionContextKey = transactionContextKey;
    }

    public static ServerSessionEvent inviteTrying(Object source, String deviceId, String callId) {
        return new ServerSessionEvent(source, SessionType.INVITE_TRYING, deviceId, callId, 0, null, null, null, null, null);
    }

    public static ServerSessionEvent inviteOk(Object source, String deviceId, String callId) {
        return new ServerSessionEvent(source, SessionType.INVITE_OK, deviceId, callId, 0, null, null, null, null, null);
    }

    public static ServerSessionEvent inviteFailure(Object source, String deviceId, String callId, int statusCode) {
        return new ServerSessionEvent(source, SessionType.INVITE_FAILURE, deviceId, callId, statusCode, null, null, null, null, null);
    }

    public static ServerSessionEvent ack(Object source, String deviceId, String callId, int statusCode) {
        return new ServerSessionEvent(source, SessionType.ACK, deviceId, callId, statusCode, null, null, null, null, null);
    }

    public static ServerSessionEvent bye(Object source, String deviceId) {
        return new ServerSessionEvent(source, SessionType.BYE, deviceId, null, 0, null, null, null, null, null);
    }

    public static ServerSessionEvent byeError(Object source, String deviceId, String errorMessage) {
        return new ServerSessionEvent(source, SessionType.BYE_ERROR, deviceId, null, 0, errorMessage, null, null, null, null);
    }

    public static ServerSessionEvent serverInvite(Object source, String callId, String fromUserId, String toUserId,
                                                  GbSessionDescription sd, String transactionContextKey) {
        return new ServerSessionEvent(source, SessionType.SERVER_INVITE, toUserId, callId, 0, null,
                fromUserId, toUserId, sd, transactionContextKey);
    }
}
