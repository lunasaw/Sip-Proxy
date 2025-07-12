package io.github.lunasaw.gbproxy.client.transmit.response.ack;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * ACK响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过AckProcessorHandler接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component
public class AckResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "ACK";

    private String method = METHOD;

    @Autowired
    private AckProcessorHandler ackProcessorHandler;

    /**
     * 处理ACK响应
     *
     * @param evt 响应事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            CallIdHeader callIdHeader = (CallIdHeader) evt.getResponse().getHeader(CallIdHeader.NAME);
            String callId = callIdHeader != null ? callIdHeader.getCallId() : null;

            if (callId != null) {
                ackProcessorHandler.handleAckResponse(callId, evt);
                log.debug("处理ACK响应：callId = {}", callId);
            } else {
                log.warn("ACK响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理ACK响应异常：evt = {}", evt, e);
        }
    }
}
