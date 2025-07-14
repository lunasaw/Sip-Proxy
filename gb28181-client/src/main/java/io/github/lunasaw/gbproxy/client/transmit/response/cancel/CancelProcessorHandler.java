package io.github.lunasaw.gbproxy.client.transmit.response.cancel;

import javax.sip.ResponseEvent;

/**
 * CANCEL响应处理器业务接口
 *
 * @author luna
 */
public interface CancelProcessorHandler {

    /**
     * 处理CANCEL响应
     *
     * @param callId     呼叫ID
     * @param statusCode 状态码
     * @param evt        响应事件
     */
    default void handleCancelResponse(String callId, int statusCode, ResponseEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }
}