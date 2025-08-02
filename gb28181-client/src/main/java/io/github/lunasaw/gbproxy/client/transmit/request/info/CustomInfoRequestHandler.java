package io.github.lunasaw.gbproxy.client.transmit.request.info;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * INFO请求业务处理器默认实现
 * 负责处理INFO请求的具体业务逻辑
 * 业务接入方可以通过实现InfoRequestHandler接口来自定义业务逻辑
 * 按照项目规范，使用Handler命名
 *
 * @author luna
 * @date 2023/12/29
 */
@Slf4j
@Component
@ConditionalOnMissingBean(InfoRequestHandler.class)
public class CustomInfoRequestHandler implements InfoRequestHandler {

    /**
     * 接收INFO消息
     * 处理INFO请求的具体业务逻辑
     *
     * @param userId  用户ID
     * @param content INFO消息内容
     */
    @Override
    public void receiveInfo(String userId, String content) {
        log.info("收到INFO消息: userId={}, content={}", userId, content);
        // 业务接入方可以在这里实现具体的业务逻辑
        // 例如：处理设备控制命令、状态更新、配置变更等
    }
}