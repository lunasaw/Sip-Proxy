package io.github.lunasaw.gbproxy.server.transimit.response.invite;

import javax.sip.ResponseEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义INVITE响应处理器实现
 *
 * @author luna
 */
@Slf4j
public class CustomInviteResponseProcessorServer implements InviteResponseProcessorServer {

    @Override
    public void responseTrying() {
        log.debug("处理INVITE Trying响应");
    }

    @Override
    public void handleOkResponse(ResponseEvent evt, String callId) {
        log.info("处理INVITE OK响应：callId = {}", callId);
    }

    @Override
    public void handleFailureResponse(ResponseEvent evt, String callId, int statusCode) {
        log.warn("处理INVITE失败响应：callId = {}, statusCode = {}", callId, statusCode);
    }
}
