package io.github.lunasaw.gbproxy.server.transmit.response.invite;

import gov.nist.javax.sip.ResponseEventExt;
import javax.sip.ResponseEvent;

/**
 * INVITE响应处理器业务接口
 * 负责具体的业务逻辑处理
 *
 * @author luna
 */
public interface InviteResponseProcessorHandler {

    /**
     * 处理Trying响应
     *
     * @param evt 响应事件
     * @param callId 呼叫ID
     */
    default void handleTryingResponse(ResponseEvent evt, String callId) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理OK响应
     *
     * @param evt 响应事件
     * @param callId 呼叫ID
     */
    default void handleOkResponse(ResponseEvent evt, String callId) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理OK响应的协议层处理
     *
     * @param evt    响应事件扩展
     * @param callId 呼叫ID
     */
    default void processOkResponse(ResponseEventExt evt, String callId) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理失败响应
     *
     * @param evt 响应事件
     * @param callId 呼叫ID
     * @param statusCode 状态码
     */
    default void handleFailureResponse(ResponseEvent evt, String callId, int statusCode) {
        // 默认实现为空，由业务方根据需要实现
    }
}