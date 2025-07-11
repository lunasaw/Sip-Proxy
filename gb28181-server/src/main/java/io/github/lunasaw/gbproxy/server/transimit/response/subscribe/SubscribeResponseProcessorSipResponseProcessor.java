package io.github.lunasaw.gbproxy.server.transimit.response.subscribe;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;

/**
 * SIP命令类型： 收到SUBSCRIBE响应*
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class SubscribeResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    public static final String               METHOD = "SUBSCRIBE";

    private String                           method = METHOD;

    @Autowired
    private SubscribeResponseProcessorServer subscribeResponseProcessorServer;

    public SubscribeResponseProcessorSipResponseProcessor(SubscribeResponseProcessorServer subscribeResponseProcessorServer) {
        this.subscribeResponseProcessorServer = subscribeResponseProcessorServer;
    }

    /**
     * 收到SUBSCRIBE响应处理
     *
     * @param evt
     */
    @Override
    public void process(ResponseEvent evt) {
        SIPResponse response = (SIPResponse)evt.getResponse();
        if (response.getStatusCode() != Response.OK) {
            return;
        }
        DeviceSubscribe deviceSubscribe = SipUtils.parseResponse(evt, DeviceSubscribe.class);
        subscribeResponseProcessorServer.responseSubscribe(deviceSubscribe);
    }
}
