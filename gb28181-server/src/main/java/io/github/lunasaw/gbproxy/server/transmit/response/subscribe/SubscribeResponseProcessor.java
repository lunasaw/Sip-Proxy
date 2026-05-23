package io.github.lunasaw.gbproxy.server.transmit.response.subscribe;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceSubscribeResponseEvent;
import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component("serverSubscribeResponseProcessor")
public class SubscribeResponseProcessor extends ServerAbstractSipResponseProcessor {

    public static final String METHOD = "SUBSCRIBE";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId == null) {
                log.warn("SUBSCRIBE响应处理失败：callId为空");
                return;
            }

            String deviceId = SipUtils.getUserIdFromFromHeader(response);
            publisher.publishEvent(new DeviceSubscribeResponseEvent(this, deviceId, callId, statusCode));

            if (statusCode == Response.OK) {
                log.info("处理SUBSCRIBE成功响应：callId = {}", callId);
            } else {
                log.warn("处理SUBSCRIBE失败响应：callId = {}, statusCode = {}", callId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理SUBSCRIBE响应异常：evt = {}", evt, e);
        }
    }
}
