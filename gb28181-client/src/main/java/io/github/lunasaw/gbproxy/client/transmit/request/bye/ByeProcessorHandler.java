package io.github.lunasaw.gbproxy.client.transmit.request.bye;

/**
 * BYE请求业务处理器接口
 * 负责处理BYE请求的业务逻辑
 *
 * @author luna
 */
public interface ByeProcessorHandler {

    /**
     * 关闭流
     *
     * @param callId 呼叫ID
     */
    void closeStream(String callId);
}
