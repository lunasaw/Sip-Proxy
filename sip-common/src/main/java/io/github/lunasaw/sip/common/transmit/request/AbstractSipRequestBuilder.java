package io.github.lunasaw.sip.common.transmit.request;

import java.util.List;
import java.util.Optional;

import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;

import org.assertj.core.util.Lists;

import com.luna.common.check.Assert;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * SIP请求构建器抽象基类
 * 提供通用的SIP请求构建逻辑和模板方法
 *
 * @author luna
 */
public abstract class AbstractSipRequestBuilder {

    /**
     * 构建SIP请求的模板方法
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param sipMessage SIP消息内容
     * @param subscribeInfo 订阅信息（可选）
     * @return SIP请求
     */
    public Request build(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage, SubscribeInfo subscribeInfo) {
        // 参数校验
        validateParameters(fromDevice, toDevice, sipMessage);

        // 处理订阅信息
        processSubscribeInfo(sipMessage, subscribeInfo);

        // 构建基础请求
        Request request = buildBaseRequest(fromDevice, toDevice, sipMessage);

        // 添加自定义头部
        addCustomHeaders(request, sipMessage);

        // 子类特定的构建逻辑
        customizeRequest(request, fromDevice, toDevice, sipMessage);

        return request;
    }

    /**
     * 构建SIP请求的模板方法（无订阅信息）
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param sipMessage SIP消息内容
     * @return SIP请求
     */
    public Request build(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        return build(fromDevice, toDevice, sipMessage, null);
    }

    /**
     * 参数校验
     */
    protected void validateParameters(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        Assert.notNull(fromDevice, "发送设备不能为null");
        Assert.notNull(toDevice, "接收设备不能为null");
        Assert.notNull(sipMessage, "SIP消息不能为null");
    }

    /**
     * 处理订阅信息
     */
    protected void processSubscribeInfo(SipMessage sipMessage, SubscribeInfo subscribeInfo) {
        if (subscribeInfo == null) {
            return;
        }

        // 设置CallId
        Optional.ofNullable(subscribeInfo.getRequest())
            .map(SIPRequest::getCallIdHeader)
            .map(CallIdHeader::getCallId)
            .ifPresent(sipMessage::setCallId);


        // 添加过期时间头部
        if (subscribeInfo.getExpires() > 0) {
            ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
            sipMessage.addHeader(expiresHeader);
        }

        // 添加事件头部
        if (subscribeInfo.getEventType() != null && subscribeInfo.getEventId() != null) {
            EventHeader eventHeader = SipRequestUtils.createEventHeader(
                subscribeInfo.getEventType(),
                subscribeInfo.getEventId());
            sipMessage.addHeader(eventHeader);
        }

        // 添加订阅状态头部
        if (subscribeInfo.getSubscriptionState() != null) {
            SubscriptionStateHeader subscriptionStateHeader = SipRequestUtils.createSubscriptionStateHeader(
                subscribeInfo.getSubscriptionState());
            sipMessage.addHeader(subscriptionStateHeader);
        }
    }

    /**
     * 构建基础SIP请求
     */
    protected Request buildBaseRequest(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 创建CallId头部
        CallIdHeader callIdHeader = SipRequestUtils.createCallIdHeader(sipMessage.getCallId());

        // 创建请求URI
        SipURI requestUri = SipRequestUtils.createSipUri(toDevice.getUserId(), toDevice.getHostAddress());

        // 创建Via头部
        ViaHeader viaHeader = SipRequestUtils.createViaHeader(
            fromDevice.getIp(),
            fromDevice.getPort(),
            toDevice.getTransport(),
            sipMessage.getViaTag());
        List<ViaHeader> viaHeaders = Lists.newArrayList(viaHeader);

        // 创建From头部
        FromHeader fromHeader = SipRequestUtils.createFromHeader(
            fromDevice.getUserId(),
            fromDevice.getHostAddress(),
            fromDevice.getFromTag());

        // 创建To头部
        ToHeader toHeader = SipRequestUtils.createToHeader(
            toDevice.getUserId(),
            toDevice.getHostAddress(),
            toDevice.getToTag());

        // 创建MaxForwards头部
        MaxForwardsHeader maxForwards = SipRequestUtils.createMaxForwardsHeader();

        // 创建CSeq头部
        CSeqHeader cSeqHeader = SipRequestUtils.createCSeqHeader(sipMessage.getSequence(), sipMessage.getMethod());

        // 创建请求
        return SipRequestUtils.createRequest(
            requestUri,
            sipMessage.getMethod(),
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            viaHeaders,
            maxForwards,
            sipMessage.getContentTypeHeader(),
            sipMessage.getContent());
    }

    /**
     * 添加自定义头部
     */
    protected void addCustomHeaders(Request request, SipMessage sipMessage) {
        SipRequestUtils.setRequestHeader(request, sipMessage.getHeaders());
    }

    /**
     * 子类特定的请求定制化逻辑
     * 默认空实现，子类可以重写
     */
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 默认空实现
    }

    /**
     * 基于SIP响应构建请求的模板方法
     */
    public Request buildFromResponse(SipURI requestUri, SipMessage sipMessage, SIPResponse sipResponse) {
        Assert.notNull(requestUri, "请求URI不能为null");
        Assert.notNull(sipMessage, "SIP消息不能为null");
        Assert.notNull(sipResponse, "SIP响应不能为null");

        // 创建Via头部
        String hostAddress = sipResponse.getLocalAddress().getHostAddress();
        int localPort = sipResponse.getLocalPort();
        ViaHeader viaHeader = SipRequestUtils.createViaHeader(
            hostAddress,
            localPort,
            sipResponse.getTopmostViaHeader().getTransport(),
            sipMessage.getViaTag());
        List<ViaHeader> viaHeaders = Lists.newArrayList(viaHeader);

        // 创建MaxForwards头部
        MaxForwardsHeader maxForwards = SipRequestUtils.createMaxForwardsHeader();

        // 创建CSeq头部
        CSeqHeader cSeqHeader = SipRequestUtils.createCSeqHeader(sipMessage.getSequence(), sipMessage.getMethod());

        // 创建请求
        Request request = SipRequestUtils.createRequest(
            requestUri,
            sipMessage.getMethod(),
            sipResponse.getCallIdHeader(),
            cSeqHeader,
            sipResponse.getFromHeader(),
            sipResponse.getToHeader(),
            viaHeaders,
            maxForwards,
            sipMessage.getContentTypeHeader(),
            sipMessage.getContent());

        // 添加自定义头部
        addCustomHeaders(request, sipMessage);

        return request;
    }
}