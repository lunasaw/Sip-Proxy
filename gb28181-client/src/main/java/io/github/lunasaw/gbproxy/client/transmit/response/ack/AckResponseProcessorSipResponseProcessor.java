package io.github.lunasaw.gbproxy.client.transmit.response.ack;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;

import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * ACK请求响应器
 *
 * @author luna
 */
@Slf4j
@Component
@Data
public class AckResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    private static final String METHOD = "ACK";

    private String method = METHOD;

    /**
     * 处理ACK响应
     *
     * @param evt
     */
    @Override
    public void process(ResponseEvent evt) {
        CallIdHeader callIdHeader = (CallIdHeader)evt.getResponse().getHeader(CallIdHeader.NAME);
        // log.info("收到响应ACK process::evt = {} , callId = {}", evt.getResponse(), callIdHeader.getCallId());
    }

}
