package io.github.lunasaw.sip.common.transmit.event.response;


import gov.nist.javax.sip.message.SIPResponse;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**
 * @author luna
 */
@Component
public abstract class AbstractSipResponseProcessor implements SipResponseProcessor {


    public SIPResponse getResponse(ResponseEvent evt) {
        if (evt == null || evt.getResponse() == null) {
            throw new IllegalArgumentException("ResponseEvent or its response cannot be null");
        }
        return (SIPResponse) evt.getResponse();
    }

    public String getCallId(ResponseEvent evt) {
        if (evt == null) {
            throw new IllegalArgumentException("ResponseEvent cannot be null");
        }
        return getResponse(evt).getCallIdHeader().getCallId();
    }
}
