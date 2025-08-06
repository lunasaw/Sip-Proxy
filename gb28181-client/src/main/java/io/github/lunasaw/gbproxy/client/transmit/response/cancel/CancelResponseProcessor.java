package io.github.lunasaw.gbproxy.client.transmit.response.cancel;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.client.transmit.response.ClientAbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**
 * CANCEL响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过CancelProcessorHandler接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("clientCancelResponseProcessor")
public class CancelResponseProcessor extends ClientAbstractSipResponseProcessor {

    public static final String METHOD = "CANCEL";

    private String method = METHOD;

    @Autowired
    @Lazy
    private CancelProcessorHandler cancelProcessorHandler;

    /**
     * 处理CANCEL响应
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
                cancelProcessorHandler.handleCancelResponse(callId, statusCode, evt);
                log.info("处理CANCEL响应：callId = {}, statusCode = {}", callId, statusCode);
            } else {
                log.warn("CANCEL响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理CANCEL响应异常：evt = {}", evt, e);
        }
    }
}
