package io.github.lunasaw.gbproxy.client.transmit.response.register;

import javax.sip.ResponseEvent;

/**
 * Register响应处理器业务接口
 *
 * @author luna
 */
public interface RegisterProcessorHandler {

    /**
     * 过期时间
     *
     * @param userId 用户id
     * @return second time
     */
    default Integer getExpire(String userId) {
        return 3600;
    }

    /**
     * 注册成功
     *
     * @param toUserId 目标用户ID
     */
    void registerSuccess(String toUserId);

    /**
     * 处理未授权响应
     *
     * @param evt      响应事件
     * @param toUserId 目标用户ID
     * @param callId   呼叫ID
     */
    default void handleUnauthorized(ResponseEvent evt, String toUserId, String callId) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理注册失败
     *
     * @param toUserId   目标用户ID
     * @param statusCode 状态码
     */
    default void handleRegisterFailure(String toUserId, int statusCode) {
        // 默认实现为空，由业务方根据需要实现
    }
}
