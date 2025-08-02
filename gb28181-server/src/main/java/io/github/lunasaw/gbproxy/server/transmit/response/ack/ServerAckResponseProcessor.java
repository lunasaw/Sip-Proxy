package io.github.lunasaw.gbproxy.server.transmit.response.ack;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;

/**
 * ACK响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过AckProcessorHandler接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("serverAckResponseProcessor")
public class ServerAckResponseProcessor extends ServerAbstractSipResponseProcessor {

    public static final String METHOD = "ACK";

    private String method = METHOD;

    @Autowired
    private ServerAckProcessorHandler serverAckProcessorHandler;

    /**
     * 处理ACK响应
     *
     * @param evt 响应事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
            String callId = callIdHeader != null ? callIdHeader.getCallId() : null;
            int statusCode = response.getStatusCode();

            if (callId != null) {
                serverAckProcessorHandler.handleAckResponse(callId, statusCode, evt);
                log.debug("处理ACK响应：callId = {}, statusCode = {}", callId, statusCode);
            } else {
                log.warn("ACK响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理ACK响应异常：evt = {}", evt, e);
        }
    }
}
