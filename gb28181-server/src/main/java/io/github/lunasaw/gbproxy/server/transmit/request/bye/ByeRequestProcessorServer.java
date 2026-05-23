package io.github.lunasaw.gbproxy.server.transmit.request.bye;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceByeErrorEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceByeEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
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
    @Lazy
    private ServerByeProcessorHandler serverByeProcessorHandler;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String sipId = SipUtils.getUserIdFromToHeader(request);
            String userId = SipUtils.getUserIdFromFromHeader(request);

            log.debug("处理BYE请求：用户ID = {}, SIP ID = {}", userId, sipId);

            if (!serverByeProcessorHandler.validateDevicePermission(userId, sipId, evt)) {
                log.warn("BYE请求权限验证失败：用户ID = {}, SIP ID = {}", userId, sipId);
                publisher.publishEvent(new DeviceByeErrorEvent(this, userId, "权限验证失败"));
                return;
            }

            publisher.publishEvent(new DeviceByeEvent(this, userId));

        } catch (Exception e) {
            log.error("处理BYE请求异常：evt = {}", evt, e);
            String userId = SipUtils.getUserIdFromFromHeader((SIPRequest) evt.getRequest());
            publisher.publishEvent(new DeviceByeErrorEvent(this, userId, e.getMessage()));
        }
    }
}
