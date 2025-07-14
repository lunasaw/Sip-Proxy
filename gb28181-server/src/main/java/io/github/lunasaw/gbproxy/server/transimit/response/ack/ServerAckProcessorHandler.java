package io.github.lunasaw.gbproxy.server.transimit.response.ack;

import javax.sip.ResponseEvent;

/**
 * ACK响应处理器业务接口
 *
 * @author luna
 */
public interface ServerAckProcessorHandler {

    /**
     * 处理ACK响应
     *
     * @param callId     呼叫ID
     * @param statusCode 状态码
     * @param evt        响应事件
     */
    default void handleAckResponse(String callId, int statusCode, ResponseEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }
}