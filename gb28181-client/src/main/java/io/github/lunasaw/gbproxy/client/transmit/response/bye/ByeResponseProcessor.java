package io.github.lunasaw.gbproxy.client.transmit.response.bye;

import javax.sip.ResponseEvent;

import gov.nist.javax.sip.message.SIPResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;

/**
 * BYE响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过ByeProcessorHandler接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component
public class ByeResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    private ByeProcessorHandler byeProcessorHandler;

    /**
     * 处理BYE响应
     *
     * @param evt 响应事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId != null) {
                byeProcessorHandler.handleByeResponse(callId, statusCode, evt);
                log.info("处理BYE响应：callId = {}, statusCode = {}", callId, statusCode);
            } else {
                log.warn("BYE响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理BYE响应异常：evt = {}", evt, e);
        }
    }
}
