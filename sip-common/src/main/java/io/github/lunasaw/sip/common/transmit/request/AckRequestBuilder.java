package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;

import org.apache.commons.lang3.StringUtils;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * ACK请求构建器
 *
 * @author luna
 */
public class AckRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建ACK请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param callId 呼叫ID
     * @return ACK请求
     */
    public Request buildAckRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return buildAckRequest(fromDevice, toDevice, null, callId);
    }

    /**
     * 创建带内容的ACK请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param content 消息内容
     * @param callId 呼叫ID
     * @return ACK请求
     */
    public Request buildAckRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getAckBody();
        sipMessage.setMethod(Request.ACK);
        sipMessage.setCallId(callId);

        if (StringUtils.isNotBlank(content)) {
            sipMessage.setContent(content);
            sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader());
        }

        return build(fromDevice, toDevice, sipMessage);
    }

    /**
     * 基于SIP响应创建ACK请求
     *
     * @param fromDevice 发送设备
     * @param sipURI 请求URI
     * @param sipResponse SIP响应
     * @return ACK请求
     */
    public Request buildAckRequest(FromDevice fromDevice, SipURI sipURI, SIPResponse sipResponse) {
        return buildAckRequest(fromDevice, sipURI, null, sipResponse);
    }

    /**
     * 基于SIP响应创建带内容的ACK请求
     *
     * @param fromDevice 发送设备
     * @param sipURI 请求URI
     * @param content 消息内容
     * @param sipResponse SIP响应
     * @return ACK请求
     */
    public Request buildAckRequest(FromDevice fromDevice, SipURI sipURI, String content, SIPResponse sipResponse) {
        SipMessage sipMessage = SipMessage.getAckBody(sipResponse);
        sipMessage.setMethod(Request.ACK);
        sipMessage.setCallId(sipResponse.getCallId().getCallId());

        if (StringUtils.isNotBlank(content)) {
            sipMessage.setContent(content);
            sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader());
        }

        return buildFromResponse(sipURI, sipMessage, sipResponse);
    }

    @Override
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 添加User-Agent头部
        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        request.addHeader(userAgentHeader);

        // 添加Contact头部
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        request.addHeader(contactHeader);
    }
}