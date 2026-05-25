package io.github.lunasaw.gbproxy.client.transmit.response.ack;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientAckEvent;
import io.github.lunasaw.gbproxy.client.transmit.response.ClientAbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;

@Slf4j
@Getter
@Setter
@Component("clientAckResponseProcessor")
public class ClientAckResponseProcessor extends ClientAbstractSipResponseProcessor {

    public static final String METHOD = "ACK";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(ResponseEvent evt) {
        try {
            CallIdHeader callIdHeader = (CallIdHeader) evt.getResponse().getHeader(CallIdHeader.NAME);
            String callId = callIdHeader != null ? callIdHeader.getCallId() : null;

            if (callId != null) {
                publisher.publishEvent(new ClientAckEvent(this, callId));
                log.debug("处理ACK响应：callId = {}", callId);
            } else {
                log.warn("ACK响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理ACK响应异常：evt = {}", evt, e);
        }
    }
}
