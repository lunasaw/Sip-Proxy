package io.github.lunasaw.gbproxy.server.transmit.response.subscribe;

import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import lombok.extern.slf4j.Slf4j;

import javax.sip.ResponseEvent;

/**
 * 自定义SUBSCRIBE响应处理器实现
 *
 * @author luna
 */
@Slf4j
public class DefaultSubscribeResponseProcessorHandler implements SubscribeResponseProcessorHandler {

    @Override
    public void responseSubscribe(DeviceSubscribe deviceSubscribe) {
        log.info("处理订阅成功响应：deviceSubscribe = {}", deviceSubscribe);
    }

    @Override
    public void handleSubscribeFailure(ResponseEvent evt, String callId, int statusCode) {
        log.warn("处理订阅失败响应：callId = {}, statusCode = {}", callId, statusCode);
    }
}