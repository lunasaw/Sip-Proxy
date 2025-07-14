package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.header.ContactHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * BYE请求构建器
 *
 * @author luna
 */
public class ByeRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建BYE请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param callId 呼叫ID
     * @return BYE请求
     */
    public Request buildByeRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        SipMessage sipMessage = SipMessage.getByeBody();
        sipMessage.setMethod(Request.BYE);
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