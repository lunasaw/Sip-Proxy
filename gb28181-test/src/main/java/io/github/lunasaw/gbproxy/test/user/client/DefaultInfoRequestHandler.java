package io.github.lunasaw.gbproxy.test.user.client;

import io.github.lunasaw.gbproxy.client.transmit.request.info.InfoRequestHandler;
import io.github.lunasaw.sip.common.entity.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * INFO请求业务处理器测试实现
 * 用于测试环境，提供INFO请求的业务逻辑处理
 * 按照项目规范，只负责业务逻辑处理，不包含协议层面逻辑
 * 按照项目规范，使用Handler命名
 *
 * @author luna
 * @date 2023/11/7
 */
@Component
@Slf4j
public class DefaultInfoRequestHandler implements InfoRequestHandler {

    @Autowired
    @Qualifier("clientFrom")
    private Device fromDevice;

    /**
     * 接收INFO消息
     * 处理INFO请求的具体业务逻辑
     *
     * @param userId  用户ID
     * @param content INFO消息内容
     */
    @Override
    public void receiveInfo(String userId, String content) {
        log.info("收到INFO消息: userId={}, content={}, fromDevice={}", userId, content, fromDevice);
        // 测试环境下的业务逻辑处理
        // 可以在这里添加测试相关的业务逻辑
    }
}