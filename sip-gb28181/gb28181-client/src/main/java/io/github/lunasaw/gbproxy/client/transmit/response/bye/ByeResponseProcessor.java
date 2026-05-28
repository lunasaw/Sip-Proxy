package io.github.lunasaw.gbproxy.client.transmit.response.bye;

import javax.sip.ResponseEvent;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import io.github.lunasaw.gbproxy.client.transmit.response.ClientAbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 客户端 BYE 响应处理器，接收 BYE 响应并发布 {@link ClientByeEvent}。
 */
@Slf4j
@Getter
@Setter
@Component("clientByeResponseProcessor")
public class ByeResponseProcessor extends ClientAbstractSipResponseProcessor {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId != null) {
                publisher.publishEvent(new ClientByeEvent(this, callId, statusCode));
                log.info("处理BYE响应：callId = {}, statusCode = {}", callId, statusCode);
            } else {
                log.warn("BYE响应处理失败：callId为空");
            }
        } catch (Exception e) {
            log.error("处理BYE响应异常：evt = {}", evt, e);
        }
    }
}
