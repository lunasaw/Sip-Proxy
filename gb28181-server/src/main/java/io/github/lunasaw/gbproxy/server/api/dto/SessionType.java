package io.github.lunasaw.gbproxy.server.api.dto;

/**
 * 会话生命周期事件子类型，用于 {@code ServerSessionEvent} 区分 INVITE/BYE/ACK 状态机各阶段。
 *
 * @author luna
 */
public enum SessionType {
    /** INVITE 100/180/183 Trying 进行中 */
    INVITE_TRYING,
    /** INVITE 200 OK 完成 */
    INVITE_OK,
    /** INVITE 4xx/5xx/6xx 失败 */
    INVITE_FAILURE,
    /** ACK 收到 */
    ACK,
    /** BYE 正常结束 */
    BYE,
    /** BYE 异常 */
    BYE_ERROR,
    /** 服务端收到设备主动发起的 INVITE（如语音对讲） */
    SERVER_INVITE
}
