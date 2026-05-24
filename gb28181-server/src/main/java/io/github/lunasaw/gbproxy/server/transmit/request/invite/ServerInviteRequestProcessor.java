package io.github.lunasaw.gbproxy.server.transmit.request.invite;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.utils.GbSdpUtils;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端 INVITE 请求处理器：协议层立即回 100 Trying 并发布 {@link ServerInviteEvent}，
 * 业务方异步监听事件后通过 {@code transactionContextKey} 取回 RequestEvent 完成回包。
 *
 * <p>UDP 重传场景下事件可能被重复 publish，业务方按 callId 自行幂等。
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
    private ApplicationEventPublisher publisher;

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Override
    public void process(RequestEvent evt) {
        // 同 JVM 同时启用 client/server 时，To-Header 不匹配本端身份则跳过，避免重复处理
        if (!serverDeviceSupplier.checkDevice(evt)) {
            return;
        }
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);
            String callId = SipUtils.getCallId(request);

            log.info("📺 服务端收到INVITE请求: callId={}, fromUserId={}, toUserId={}", callId, fromUserId, toUserId);

            // 1. 立即发 100 Trying，防止对端按 T1 退避重传
            ResponseCmd.sendResponse(Response.TRYING, evt);

            // 2. 存事务上下文供业务方异步取回
            SipTransactionRegistry.TransactionContextInfo ctx =
                    SipTransactionRegistry.createContext(evt, evt.getServerTransaction());

            // 3. 解析 SDP 并发布事件
            GbSessionDescription sessionDescription = null;
            byte[] rawContent = request.getRawContent();
            if (rawContent != null) {
                sessionDescription = GbSdpUtils.parseGbSdp(new String(rawContent));
            }
            publisher.publishEvent(new ServerInviteEvent(this, callId, fromUserId, toUserId,
                    sessionDescription, ctx.getContextKey()));
        } catch (Exception e) {
            log.error("处理INVITE请求异常: evt = {}", evt, e);
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, "Internal Server Error", evt);
        }
    }
}
