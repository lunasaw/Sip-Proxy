package io.github.lunasaw.gbproxy.server.transmit.request.bye;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerSessionEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component("serverByeRequestProcessor")
@Getter
@Setter
@Slf4j
public class ByeRequestProcessorServer extends ServerAbstractSipRequestProcessor {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        String userId = SipUtils.getUserIdFromFromHeader(request);
        try {
            // RFC 3261 §15.1.2: 收到 BYE 必须回 200 OK，否则对端会按 T1 退避重传
            ResponseCmd.sendResponse(Response.OK, evt);
            publisher.publishEvent(ServerSessionEvent.bye(this, userId));
        } catch (Exception e) {
            log.error("处理BYE请求异常: userId={}", userId, e);
            publisher.publishEvent(ServerSessionEvent.byeError(this, userId, e.getMessage()));
        }
    }
}
