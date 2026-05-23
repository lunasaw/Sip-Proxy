package io.github.lunasaw.gbproxy.server.transmit.request.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.GbSessionDescription;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端INVITE请求处理器
 * 处理服务端收到的INVITE请求，专注于协议层面处理
 *
 * @author luna
 */
@Component("serverInviteRequestProcessor")
@Getter
@Setter
@Slf4j
public class ServerInviteRequestProcessor extends SipRequestProcessorAbstract {

    public static final String METHOD = "INVITE";

    private String method = METHOD;

    @Autowired
    @Lazy
    private ServerInviteRequestHandler serverInviteRequestHandler;

    /**
     * 处理INVITE请求
     *
     * @param evt
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 协议层面处理：解析SIP消息
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);
            String callId = SipUtils.getCallId(request);

            log.info("📺 服务端收到INVITE请求: callId={}, fromUserId={}, toUserId={}", callId, fromUserId, toUserId);

            // 解析Sdp
            String sdpContent = new String(request.getRawContent());
            GbSessionDescription sessionDescription = (GbSessionDescription) SipUtils.parseSdp(sdpContent);

            // 调用业务处理器
            serverInviteRequestHandler.inviteSession(callId, sessionDescription);
            String content = serverInviteRequestHandler.getInviteResponse(toUserId, sessionDescription);

            // 构建响应
            ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader();
            ResponseCmd.sendResponse(Response.OK, content, contentTypeHeader, evt);

            log.info("✅ 服务端INVITE请求处理完成: callId={}", callId);

        } catch (Exception e) {
            log.error("处理INVITE请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, "Internal Server Error", evt);
        }
    }
}