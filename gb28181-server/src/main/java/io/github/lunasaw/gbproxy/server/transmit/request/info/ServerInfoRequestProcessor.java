package io.github.lunasaw.gbproxy.server.transmit.request.info;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoErrorEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoRequestEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component("serverInfoRequestProcessor")
@Getter
@Setter
@Slf4j
public class ServerInfoRequestProcessor extends ServerAbstractSipRequestProcessor {

    public static final String METHOD = "INFO";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        String userId = SipUtils.getUserIdFromFromHeader(request);
        try {
            String content = request.getRawContent() != null ? new String(request.getRawContent()) : "";
            ResponseCmd.sendResponse(Response.OK, evt);
            publisher.publishEvent(new DeviceInfoRequestEvent(this, userId, content));
        } catch (Exception e) {
            log.error("处理INFO请求异常: userId={}", userId, e);
            publisher.publishEvent(new DeviceInfoErrorEvent(this, userId, e.getMessage()));
        }
    }
}
