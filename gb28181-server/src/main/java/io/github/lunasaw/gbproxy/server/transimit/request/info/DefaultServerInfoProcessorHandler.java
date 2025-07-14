package io.github.lunasaw.gbproxy.server.transimit.request.info;

import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * Server模块INFO请求处理器业务接口默认实现
 *
 * @author luna
 */
@Slf4j
public class DefaultServerInfoProcessorHandler implements ServerInfoProcessorHandler {

    @Override
    public void handleInfoRequest(String userId, String content, RequestEvent evt) {
        log.debug("默认处理INFO请求：用户ID = {}, 内容 = {}", userId, content);
    }

    @Override
    public boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        log.debug("默认验证设备权限：用户ID = {}, SIP ID = {}", userId, sipId);
        return true;
    }

    @Override
    public void handleInfoError(String userId, String errorMessage, RequestEvent evt) {
        log.debug("默认处理INFO请求错误：用户ID = {}, 错误消息 = {}", userId, errorMessage);
    }
}