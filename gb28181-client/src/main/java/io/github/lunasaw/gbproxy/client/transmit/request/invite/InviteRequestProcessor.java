package io.github.lunasaw.gbproxy.client.transmit.request.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.utils.GbSdpUtils;
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
@Component("clientInviteRequestProcessor")
@Getter
@Setter
@Slf4j
public class InviteRequestProcessor extends SipRequestProcessorAbstract {

    public static final String    METHOD = "INVITE";

    private String                method = METHOD;

    @Autowired
    @Lazy
    private InviteRequestHandler inviteRequestHandler;

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

            log.info("📺 客户端收到INVITE请求: callId={}, fromUserId={}, toUserId={}", callId, toUserId, userId);

            // 解析Sdp
            String sdpContent = new String(request.getRawContent());
            GbSessionDescription sessionDescription = GbSdpUtils.parseGbSdp(sdpContent);

            // 调用业务处理器
            inviteRequestHandler.inviteSession(callId, sessionDescription);
            String content = inviteRequestHandler.getInviteResponse(userId, sessionDescription);

            // 构建响应
            ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader();
            ResponseCmd.sendResponse(Response.OK, content, contentTypeHeader, evt);

            log.info("✅ 客户端INVITE请求处理完成: callId={}", callId);

        } catch (Exception e) {
            log.error("处理INVITE请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, "Internal Server Error", evt);
        }
    }
}
