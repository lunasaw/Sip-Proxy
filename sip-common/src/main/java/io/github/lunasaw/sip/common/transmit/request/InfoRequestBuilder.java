package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.header.ContactHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * INFO请求构建器
 *
 * @author luna
 */
public class InfoRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建INFO请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param content 消息内容
     * @param callId 呼叫ID
     * @return INFO请求
     */
    public Request buildInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getInfoBody();
        sipMessage.setMethod(Request.INFO);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        return build(fromDevice, toDevice, sipMessage);
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