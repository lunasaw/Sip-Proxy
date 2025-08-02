package io.github.lunasaw.gbproxy.server.transmit.request.bye;

import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * Server模块BYE请求处理器业务接口默认实现
 *
 * @author luna
 */
@Slf4j
public class DefaultServerByeProcessorHandler implements ServerByeProcessorHandler {

    @Override
    public void handleByeRequest(String userId, RequestEvent evt) {
        log.debug("默认处理BYE请求：用户ID = {}, 事件 = {}", userId, evt);
    }

    @Override
    public boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        log.debug("默认验证设备权限：用户ID = {}, SIP ID = {}", userId, sipId);
        return true;
    }

    @Override
    public void handleByeError(String userId, String errorMessage, RequestEvent evt) {
        log.debug("默认处理BYE请求错误：用户ID = {}, 错误消息 = {}", userId, errorMessage);
    }
}