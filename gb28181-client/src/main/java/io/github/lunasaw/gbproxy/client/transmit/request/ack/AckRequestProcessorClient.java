package io.github.lunasaw.gbproxy.client.transmit.request.ack;

import javax.sip.RequestEvent;

/**
 * ACK请求业务处理器接口
 * 负责处理ACK请求的业务逻辑
 *
 * @author weidian
 */
public interface AckRequestProcessorClient {

    /**
     * 处理ACK请求
     *
     * @param evt ACK请求事件
     */
    void processAck(RequestEvent evt);
}
