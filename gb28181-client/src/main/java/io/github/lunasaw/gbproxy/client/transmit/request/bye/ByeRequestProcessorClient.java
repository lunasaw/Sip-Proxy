package io.github.lunasaw.gbproxy.client.transmit.request.bye;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientByeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component("byeRequestProcessorClient")
@Getter
@Setter
@Slf4j
public class ByeRequestProcessorClient extends SipRequestProcessorAbstract {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);
            String callId = SipUtils.getCallId(request);

            log.debug("收到BYE请求: from={}, to={}, callId={}", fromUserId, toUserId, callId);

            ResponseCmd.sendResponse(Response.OK, evt);

            Dialog dialog = evt.getDialog();
            if (dialog != null) {
                publisher.publishEvent(new ClientByeEvent(this, dialog.getCallId().getCallId(), Response.OK));
            }

        } catch (Exception e) {
            log.error("处理BYE请求时发生异常: evt = {}", evt, e);
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, evt);
        }
    }
}
