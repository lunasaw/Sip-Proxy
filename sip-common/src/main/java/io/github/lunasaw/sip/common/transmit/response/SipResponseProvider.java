package io.github.lunasaw.sip.common.transmit.response;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import java.util.ArrayList;

import javax.sip.header.*;
import javax.sip.message.Response;
import java.util.List;

/**
 * @author luna
 * @date 2023/10/18
 */
public class SipResponseProvider {

    public static Response createSipResponse(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        CallIdHeader callIdHeader = SipRequestUtils.createCallIdHeader(sipMessage.getCallId());
        // via
        ViaHeader viaHeader =
                SipRequestUtils.createViaHeader(fromDevice.getIp(), fromDevice.getPort(), toDevice.getTransport(), sipMessage.getViaTag());
        List<ViaHeader> viaHeaders = new ArrayList<>(java.util.Arrays.asList(viaHeader));
        // from
        FromHeader fromHeader = SipRequestUtils.createFromHeader(fromDevice.getUserId(), fromDevice.getHostAddress(), fromDevice.getFromTag());
        // to
        ToHeader toHeader = SipRequestUtils.createToHeader(toDevice.getUserId(), toDevice.getHostAddress(), toDevice.getToTag());
        // Forwards
        MaxForwardsHeader maxForwards = SipRequestUtils.createMaxForwardsHeader();
        // ceq
        CSeqHeader cSeqHeader = SipRequestUtils.createCSeqHeader(sipMessage.getSequence(), sipMessage.getMethod());
        // request
        Response response = SipRequestUtils.createResponse(sipMessage.getStatusCode(), callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards, sipMessage.getContentTypeHeader(), sipMessage.getContent());

        SipRequestUtils.setResponseHeader(response, sipMessage.getHeaders());
        return response;
    }


    /**
     * 创建响应
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param callId     callId
     * @return Request
     */
    public static Response createOkResponse(FromDevice fromDevice, ToDevice toDevice, String method, Integer statusCode, String callId) {
        SipMessage sipMessage = SipMessage.getResponse(statusCode);
        sipMessage.setCallId(callId);
        sipMessage.setMethod(method);
        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());

        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipResponse(fromDevice, toDevice, sipMessage);
    }
}
