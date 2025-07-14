package io.github.lunasaw.sip.common.transmit.request;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.Request;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.util.DigestUtils;

import com.luna.common.check.Assert;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * Sip命令request创造器
 *
 * @deprecated 请使用 {@link SipRequestBuilderFactory} 替代此类
 * 此类保留是为了向后兼容，新代码建议使用新的构建器模式
 */
@Deprecated
public class SipRequestProvider {

    /**
     * 带订阅创建SIP请求
     *
     * @param fromDevice    发送设备
     * @param toDevice      发送目的设备
     * @param sipMessage    内容
     * @param subscribeInfo 订阅消息
     * @return
     */
    public static Request createSipRequest(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage, SubscribeInfo subscribeInfo) {
        if (subscribeInfo != null) {

            Optional.ofNullable(subscribeInfo.getRequest()).map(SIPRequest::getCallIdHeader).map(CallIdHeader::getCallId)
                    .ifPresent(sipMessage::setCallId);
            Optional.ofNullable(subscribeInfo.getResponse()).map(SIPResponse::getToTag).ifPresent(fromDevice::setFromTag);
            Optional.ofNullable(subscribeInfo.getRequest()).map(SIPRequest::getFromTag).ifPresent(toDevice::setToTag);

            if (subscribeInfo.getExpires() > 0) {
                ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
                sipMessage.addHeader(expiresHeader);
            }

            if (subscribeInfo.getEventType() != null && subscribeInfo.getEventId() != null) {
                EventHeader eventHeader = SipRequestUtils.createEventHeader(subscribeInfo.getEventType(), subscribeInfo.getEventId());
                sipMessage.addHeader(eventHeader);
            }

            if (subscribeInfo.getSubscriptionState() != null) {
                SubscriptionStateHeader subscriptionStateHeader = SipRequestUtils.createSubscriptionStateHeader(subscribeInfo.getSubscriptionState());
                sipMessage.addHeader(subscriptionStateHeader);
            }
        }

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 创建SIP请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param sipMessage 内容
     * @return Request
     */
    public static Request createSipRequest(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        Assert.notNull(fromDevice, "发送设备不能为null");
        Assert.notNull(toDevice, "发送设备不能为null");

        CallIdHeader callIdHeader = SipRequestUtils.createCallIdHeader(sipMessage.getCallId());
        // sipUri
        SipURI requestUri = SipRequestUtils.createSipUri(toDevice.getUserId(), toDevice.getHostAddress());
        // via
        ViaHeader viaHeader =
                SipRequestUtils.createViaHeader(fromDevice.getIp(), fromDevice.getPort(), toDevice.getTransport(), sipMessage.getViaTag());
        List<ViaHeader> viaHeaders = Lists.newArrayList(viaHeader);
        // from
        FromHeader fromHeader = SipRequestUtils.createFromHeader(fromDevice.getUserId(), fromDevice.getHostAddress(), fromDevice.getFromTag());
        // to
        ToHeader toHeader = SipRequestUtils.createToHeader(toDevice.getUserId(), toDevice.getHostAddress(), toDevice.getToTag());
        // Forwards
        MaxForwardsHeader maxForwards = SipRequestUtils.createMaxForwardsHeader();
        // ceq
        CSeqHeader cSeqHeader = SipRequestUtils.createCSeqHeader(sipMessage.getSequence(), sipMessage.getMethod());
        // request
        Request request = SipRequestUtils.createRequest(requestUri, sipMessage.getMethod(), callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards, sipMessage.getContentTypeHeader(), sipMessage.getContent());

        SipRequestUtils.setRequestHeader(request, sipMessage.getHeaders());
        return request;
    }

    public static Request createSipRequest(SipURI requestUri, SipMessage sipMessage, SIPResponse sipResponse) {
        Assert.notNull(requestUri, "发送设备不能为null");
        Assert.notNull(sipMessage, "发送设备不能为null");

        // via
        String hostAddress = sipResponse.getLocalAddress().getHostAddress();
        int localPort = sipResponse.getLocalPort();
        ViaHeader viaHeader =
                SipRequestUtils.createViaHeader(hostAddress, localPort, sipResponse.getTopmostViaHeader().getTransport(), sipMessage.getViaTag());
        List<ViaHeader> viaHeaders = Lists.newArrayList(viaHeader);

        // Forwards
        MaxForwardsHeader maxForwards = SipRequestUtils.createMaxForwardsHeader();
        // ceq
        CSeqHeader cSeqHeader = SipRequestUtils.createCSeqHeader(sipMessage.getSequence(), sipMessage.getMethod());
        // request
        Request request = SipRequestUtils.createRequest(requestUri, sipMessage.getMethod(), sipResponse.getCallIdHeader(), cSeqHeader, sipResponse.getFromHeader(),
                sipResponse.getToHeader(), viaHeaders, maxForwards, sipMessage.getContentTypeHeader(), sipMessage.getContent());

        SipRequestUtils.setRequestHeader(request, sipMessage.getHeaders());
        return request;
    }

    /**
     * 创建Message请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param content    内容
     * @param callId     callId
     * @return Request
     */
    public static Request createMessageRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getMessageBody();
        sipMessage.setMethod(Request.MESSAGE);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        sipMessage.addHeader(userAgentHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 创建Invite请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param content    内容
     * @param callId     callId
     * @return Request
     */
    public static Request createInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        SipMessage sipMessage = SipMessage.getInviteBody();
        sipMessage.setMethod(Request.INVITE);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        SubjectHeader subjectHeader = SipRequestUtils.createSubjectHeader(subject);

        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader).addHeader(subjectHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    public Request createPlaybackInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return createInviteRequest(fromDevice, toDevice, content, subject, callId);
    }

    /**
     * 创建Bye请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param callId     callId
     * @return Request
     */
    public static Request createByeRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {

        SipMessage sipMessage = SipMessage.getByeBody();
        sipMessage.setMethod(Request.BYE);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());

        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 创建Register请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param callId     callId
     * @return Request
     */
    public static Request createRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires, String callId) {

        SipMessage sipMessage = SipMessage.getRegisterBody();
        sipMessage.setMethod(Request.REGISTER);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(expires);

        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader).addHeader(expiresHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 带签名的注册构造器
     *
     * @param www 认证头
     * @return Request
     */
    public static Request createRegisterRequestWithAuth(FromDevice fromDevice, ToDevice toDevice, String callId, Integer expires,
                                                        WWWAuthenticateHeader www) {

        return SipRequestBuilderFactory.createRegisterRequestWithAuth(fromDevice, toDevice, callId, expires, www);
    }

    /**
     * 创建Subscribe请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param content    内容
     * @param callId     callId
     * @return Request
     */
    public static Request createSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getSubscribeBody();
        sipMessage.setMethod(Request.SUBSCRIBE);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());

        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 创建INFO 请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param content    内容
     * @param callId     callId
     * @return Request
     */
    public static Request createInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {

        SipMessage sipMessage = SipMessage.getInfoBody();
        sipMessage.setMethod(Request.INFO);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());

        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    /**
     * 创建ACK请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param callId     callId
     * @return Request
     */
    public static Request createAckRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return createAckRequest(fromDevice, toDevice, null, callId);
    }

    public static Request createAckRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getAckBody();
        sipMessage.setMethod(Request.ACK);
        sipMessage.setCallId(callId);

        if (StringUtils.isNotBlank(content)) {
            sipMessage.setContent(content);
            sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader());
        }

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());

        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }

    public static Request createAckRequest(FromDevice fromDevice, SipURI sipURI, SIPResponse sipResponse) {
        return createAckRequest(fromDevice, sipURI, null, sipResponse);
    }


    public static Request createAckRequest(FromDevice fromDevice, SipURI sipURI, String content, SIPResponse sipResponse) {
        SipMessage sipMessage = SipMessage.getAckBody(sipResponse);
        sipMessage.setMethod(Request.ACK);
        sipMessage.setCallId(sipResponse.getCallId().getCallId());

        if (StringUtils.isNotBlank(content)) {
            sipMessage.setContent(content);
            sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader());
        }

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());

        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(sipURI, sipMessage, sipResponse);
    }


    /**
     * 创建Notify请求
     *
     * @param fromDevice 发送设备
     * @param toDevice   发送目的设备
     * @param content    内容
     * @param callId     callId
     * @return Request
     */
    public static Request createNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getNotifyBody();
        sipMessage.setMethod(Request.NOTIFY);
        sipMessage.setCallId(callId);
        sipMessage.setContent(content);

        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        sipMessage.addHeader(userAgentHeader).addHeader(contactHeader);

        return createSipRequest(fromDevice, toDevice, sipMessage);
    }
}
