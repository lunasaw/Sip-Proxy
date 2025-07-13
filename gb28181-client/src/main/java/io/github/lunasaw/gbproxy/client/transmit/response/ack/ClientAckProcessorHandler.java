package io.github.lunasaw.gbproxy.client.transmit.response.ack;

import javax.sip.ResponseEvent;

/**
 * ACK响应处理器业务接口
 *
 * @author luna
 */
public interface ClientAckProcessorHandler {

    /**
     * 处理ACK响应
     *
     * @param callId 呼叫ID
     * @param evt    响应事件
     */
    default void handleAckResponse(String callId, ResponseEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }
}