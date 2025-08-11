package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.client.transmit.response.register.RegisterProcessorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**
 * 测试专用的 RegisterProcessorHandler 实现
 * 用于客户端注册响应处理的测试
 *
 * @author claude
 * @date 2025/01/23
 */
@Component
@Primary
@Slf4j
public class TestRegisterProcessorHandler implements RegisterProcessorHandler {

    @Override
    public void registerSuccess(String toUserId) {
        log.info("🎉 TestRegisterProcessorHandler - 注册成功: toUserId={}", toUserId);
    }

    @Override
    public void handleUnauthorized(ResponseEvent evt, String toUserId, String callId) {
        log.info("🔐 TestRegisterProcessorHandler - 处理未授权: toUserId={}, callId={}", toUserId, callId);
    }

    @Override
    public void handleRegisterFailure(String toUserId, int statusCode) {
        log.info("❌ TestRegisterProcessorHandler - 注册失败: toUserId={}, statusCode={}", toUserId, statusCode);
    }
}