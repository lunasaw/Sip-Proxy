package io.github.lunasaw.gbproxy.client.transmit.request.invite;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.GbSessionDescription;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * SIP命令类型： 收到Invite请求
 * 客户端发起Invite请求, Invite Request消息实现，请求视频指令
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class InviteRequestProcessor extends SipRequestProcessorAbstract {

    public static final String    METHOD = "INVITE";

    private String                method = METHOD;

    @Autowired
    private InviteProcessorClient inviteProcessorClient;

    /**
     * 收到Invite请求 处理
     *
     * @param evt
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 协议层面处理：解析SIP消息
            String toUserId = SipUtils.getUserIdFromFromHeader(request);
            String userId = SipUtils.getUserIdFromToHeader(request);
            String callId = SipUtils.getCallId(request);

            // 解析Sdp
            GbSessionDescription sessionDescription = (GbSessionDescription) SipUtils.parseSdp(new String(request.getRawContent()));

            // 调用业务处理器
            inviteProcessorClient.inviteSession(callId, sessionDescription);
            String content = inviteProcessorClient.getInviteResponse(userId, sessionDescription);

            // 构建响应
            ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader();
            ResponseCmd.doResponseCmd(Response.OK, "OK", content, contentTypeHeader, evt);

        } catch (Exception e) {
            log.error("处理INVITE请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.doResponseCmd(Response.SERVER_INTERNAL_ERROR, "Internal Server Error", evt);
        }
    }
}
