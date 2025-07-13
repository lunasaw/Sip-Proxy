package io.github.lunasaw.gbproxy.test.user.server;

import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.server.transimit.response.invite.InviteResponseProcessorHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sip.ResponseEvent;

/**
 * 测试用的INVITE响应处理器业务实现
 *
 * @author luna
 * @date 2023/10/21
 */
@Component
@Slf4j
public class DefaultInviteResponseProcessorServer implements InviteResponseProcessorHandler {

    @Override
    public void handleTryingResponse(ResponseEvent evt, String callId) {
        log.info("handleTryingResponse:: callId = {}", callId);
    }

    @Override
    public void handleOkResponse(ResponseEvent evt, String callId) {
        log.info("handleOkResponse:: callId = {}", callId);
    }

    @Override
    public void handleFailureResponse(ResponseEvent evt, String callId, int statusCode) {
        log.info("handleFailureResponse:: callId = {}, statusCode = {}", callId, statusCode);
    }
}
