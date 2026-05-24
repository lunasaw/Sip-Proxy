package io.github.lunasaw.gbproxy.client.transmit.request.info;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端 INFO 请求处理器：直接回 200 OK 并发布 {@link ClientInfoEvent}。
 *
 * @author luna
 */
@Component("clientInfoRequestProcessor")
@Getter
@Setter
@Slf4j
public class InfoRequestProcessor extends SipRequestProcessorAbstract {

    public static final String METHOD = "INFO";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String userId = SipUtils.getUserIdFromToHeader(request);
            String content = request.getRawContent() != null ? new String(request.getRawContent()) : "";

            ResponseCmd.sendResponse(Response.OK, evt);
            publisher.publishEvent(new ClientInfoEvent(this, userId, content));
        } catch (Exception e) {
            log.error("处理INFO请求异常: evt = {}", evt, e);
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, e.getMessage(), evt);
        }
    }
}
