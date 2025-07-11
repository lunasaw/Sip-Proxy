package io.github.lunasaw.gbproxy.client.transmit.response.bye;

import javax.sip.ResponseEvent;

import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import lombok.Data;

/**
 * BYE请求响应器
 *
 * @author luna
 */
@Component
@Data
@Slf4j
public class ByeResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    /**
     * 处理BYE响应
     *
     * @param evt
     */
    @Override
    public void process(ResponseEvent evt) {
        SIPResponse response = (SIPResponse) evt.getResponse();
        String callId = response.getCallIdHeader().getCallId();
        log.info("process::evt = {}", evt);
    }

}
