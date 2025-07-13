package io.github.lunasaw.gbproxy.client.transmit.response.bye;

import javax.sip.ResponseEvent;

/**
 * BYE响应处理器业务接口
 *
 * @author luna
 */
public interface ByeProcessorHandler {

    /**
     * 处理BYE响应
     *
     * @param callId     呼叫ID
     * @param statusCode 状态码
     * @param evt        响应事件
     */
    default void handleByeResponse(String callId, int statusCode, ResponseEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    void closeStream(String callId);
}