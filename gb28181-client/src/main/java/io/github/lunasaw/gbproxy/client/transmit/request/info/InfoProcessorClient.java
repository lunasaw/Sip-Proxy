package io.github.lunasaw.gbproxy.client.transmit.request.info;

/**
 * INFO请求业务处理器接口
 * 负责处理INFO请求的业务逻辑
 *
 * @author luna
 */
public interface InfoProcessorClient {

    /**
     * 接收INFO消息
     *
     * @param userId  用户ID
     * @param content INFO消息内容
     */
    void receiveInfo(String userId, String content);
}
