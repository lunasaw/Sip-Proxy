package io.github.lunasaw.gbproxy.client.transmit.response.bye;

import javax.sip.ResponseEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认BYE处理器实现
 *
 * @author luna
 */
@Slf4j
@Component
public class DefaultClientByeProcessorHandler implements ClientByeProcessorHandler {

    @Override
    public void handleByeResponse(String callId, int statusCode, ResponseEvent evt) {
        log.debug("处理BYE响应：callId = {}, statusCode = {}", callId, statusCode);
        // 默认实现为空，业务方可以根据需要重写此方法
    }

    @Override
    public void closeStream(String callId) {

    }
}