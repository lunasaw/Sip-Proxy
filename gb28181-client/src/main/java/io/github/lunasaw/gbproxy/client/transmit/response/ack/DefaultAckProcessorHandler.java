package io.github.lunasaw.gbproxy.client.transmit.response.ack;

import javax.sip.ResponseEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认ACK处理器实现
 *
 * @author luna
 */
@Slf4j
@Component
public class DefaultAckProcessorHandler implements ClientAckProcessorHandler {

    @Override
    public void handleAckResponse(String callId, ResponseEvent evt) {
        log.debug("处理ACK响应：callId = {}", callId);
        // 默认实现为空，业务方可以根据需要重写此方法
    }
}