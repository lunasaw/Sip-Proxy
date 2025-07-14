package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.header.ContactHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * INVITE请求构建器
 *
 * @author luna
 */
public class InviteRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建INVITE请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param content 消息内容
     * @param subject 主题
     * @param callId 呼叫ID
     * @return INVITE请求
     */
    public Request buildInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        SipMessage sipMessage = SipMessage.getInviteBody();
        sipMessage.setMethod(Request.INVITE);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        // 临时设置subject到toDevice，用于构建请求
        String originalSubject = toDevice.getSubject();
        try {
            toDevice.setSubject(subject);
            return build(fromDevice, toDevice, sipMessage);
        } finally {
            // 恢复原始subject
            toDevice.setSubject(originalSubject);
        }
    }

    /**
     * 创建回放INVITE请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param content 消息内容
     * @param subject 主题
     * @param callId 呼叫ID
     * @return 回放INVITE请求
     */
    public Request buildPlaybackInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return buildInviteRequest(fromDevice, toDevice, content, subject, callId);
    }

    @Override
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 添加User-Agent头部
        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        request.addHeader(userAgentHeader);

        // 添加Contact头部
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        request.addHeader(contactHeader);

        // 添加Subject头部（如果有的话）
        if (toDevice.getSubject() != null) {
            SubjectHeader subjectHeader = SipRequestUtils.createSubjectHeader(toDevice.getSubject());
            request.addHeader(subjectHeader);
        }
    }
}