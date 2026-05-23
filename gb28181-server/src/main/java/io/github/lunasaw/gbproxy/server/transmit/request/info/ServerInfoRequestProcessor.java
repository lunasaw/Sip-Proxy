package io.github.lunasaw.gbproxy.server.transmit.request.info;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoErrorEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoRequestEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
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
    @Lazy
    private ServerInfoProcessorHandler serverInfoProcessorHandler;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String sipId = SipUtils.getUserIdFromToHeader(request);
            String userId = SipUtils.getUserIdFromFromHeader(request);

            log.debug("处理INFO请求：用户ID = {}, SIP ID = {}", userId, sipId);

            if (!serverInfoProcessorHandler.validateDevicePermission(userId, sipId, evt)) {
                log.warn("INFO请求权限验证失败：用户ID = {}, SIP ID = {}", userId, sipId);
                publisher.publishEvent(new DeviceInfoErrorEvent(this, userId, "权限验证失败"));
                return;
            }

            String content = evt.getRequest().getRawContent() != null
                    ? new String(evt.getRequest().getRawContent()) : "";
            publisher.publishEvent(new DeviceInfoRequestEvent(this, userId, content));

        } catch (Exception e) {
            log.error("处理INFO请求异常：evt = {}", evt, e);
            String userId = SipUtils.getUserIdFromFromHeader((SIPRequest) evt.getRequest());
            publisher.publishEvent(new DeviceInfoErrorEvent(this, userId, e.getMessage()));
        }
    }
}
