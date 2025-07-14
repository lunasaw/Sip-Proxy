package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;

import com.luna.common.check.Assert;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * SUBSCRIBE请求构建器
 *
 * @author luna
 */
public class SubscribeRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建SUBSCRIBE请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param content 消息内容
     * @param subscribeInfo 订阅信息
     * @param callId 呼叫ID
     * @return SUBSCRIBE请求
     */
    public Request buildSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content, SubscribeInfo subscribeInfo, String callId) {
        Assert.notNull(subscribeInfo, "subscribeInfo is null");

        SipMessage sipMessage = SipMessage.getSubscribeBody();
        sipMessage.setMethod(Request.SUBSCRIBE);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        return build(fromDevice, toDevice, sipMessage, subscribeInfo);
    }

    @Override
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 添加User-Agent头部
        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        request.addHeader(userAgentHeader);

        // 添加Contact头部
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        request.addHeader(contactHeader);

        // 添加Event头部（如果有的话）
        if (toDevice.getEventType() != null) {
            EventHeader eventHeader = SipRequestUtils.createEventHeader(toDevice.getEventType(), toDevice.getEventId());
            request.addHeader(eventHeader);
        }
    }
}