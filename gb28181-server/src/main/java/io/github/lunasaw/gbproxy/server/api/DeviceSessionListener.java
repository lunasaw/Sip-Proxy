package io.github.lunasaw.gbproxy.server.api;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;

/**
 * INVITE / BYE / 流媒体会话监听器（server 角色业务方实现）。
 *
 * <p>覆盖 SIP INVITE 三向握手 + BYE + ACK 状态机的关键节点。fire-and-forget。
 *
 * <p>UDP 重传场景下事件可能被多次发布，业务方需按 callId 自行幂等。
 *
 * @author luna
 */
public interface DeviceSessionListener {

    /** INVITE 收到 100 Trying。 */
    default void onInviteTrying(String deviceId, String callId) {}

    /** INVITE 200 OK 完成（媒体会话建立）。 */
    default void onInviteOk(String deviceId, String callId) {}

    /** INVITE 失败（4xx/5xx/6xx）。 */
    default void onInviteFailure(String deviceId, String callId, int statusCode) {}

    /** 收到 ACK（INVITE 三向握手最后一步），statusCode 为对应 INVITE 响应码。 */
    default void onAck(String deviceId, String callId, int statusCode) {}

    /** BYE 正常结束。 */
    default void onBye(String deviceId) {}

    /** BYE 异常（对端 4xx/timeout）。 */
    default void onByeError(String deviceId, String errorMessage) {}

    /**
     * 服务端收到设备主动发起的 INVITE（如语音对讲场景）。
     * 业务方收到后可异步准备 SDP，通过 {@code transactionContextKey} 取回 RequestEvent 完成回包。
     */
    default void onServerInvite(String callId, String fromUserId, String toUserId,
                                GbSessionDescription sessionDescription, String transactionContextKey) {}
}
