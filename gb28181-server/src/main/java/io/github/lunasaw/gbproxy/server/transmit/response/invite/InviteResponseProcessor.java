package io.github.lunasaw.gbproxy.server.transmit.response.invite;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerSessionEvent;
import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.Dialog;
import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * INVITE响应处理器
 * 协议层处理：必须执行的 ACK 回复在此完成；业务通知通过 ApplicationEvent 发布。
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("serverInviteResponseProcessor")
public class InviteResponseProcessor extends ServerAbstractSipResponseProcessor {

    public static final String METHOD = "INVITE";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 处理INVITE响应
     *
     * @param evt 响应消息
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId == null) {
                log.warn("INVITE响应处理失败：callId为空");
                return;
            }

            String deviceId = SipUtils.getUserIdFromToHeader(response);

            if (statusCode == Response.TRYING) {
                publisher.publishEvent(ServerSessionEvent.inviteTrying(this, deviceId, callId));
                log.debug("处理INVITE Trying响应：callId = {}", callId);
            } else if (statusCode == Response.OK) {
                sendAck((ResponseEventExt) evt, callId);
                publisher.publishEvent(ServerSessionEvent.inviteOk(this, deviceId, callId));
                log.info("处理INVITE OK响应：callId = {}", callId);
            } else {
                publisher.publishEvent(ServerSessionEvent.inviteFailure(this, deviceId, callId, statusCode));
                log.warn("处理INVITE失败响应：callId = {}, statusCode = {}", callId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理INVITE响应异常：evt = {}", evt, e);
        }
    }

    /**
     * 协议层 ACK 回复：基于 JAIN-SIP Dialog API 发送（1.7.0 改造）。
     *
     * <p>1.7.0 之前：手动解析 200 OK 的 SDP 取 username + remote ip:port 构造 SipURI，再调
     * {@code SipSender.doAckRequest(from, sipURI, response)}。该路径不依赖 dialog，但 SDP 异常 /
     * 设备 NAT IP 切换时构造逻辑容易出错。
     *
     * <p>1.7.0：直接走 {@code dialog.sendAck(dialog.createAck(cseq))}，与 BYE 路径对称。dialog 由
     * INVITE stateful 发送时自动创建并注册到 DialogRegistry，{@code evt.getDialog()} 直接可用。
     */
    private void sendAck(ResponseEventExt evt, String callId) {
        try {
            Dialog dialog = evt.getDialog();
            if (dialog == null) {
                log.warn("INVITE 200 OK 不带 dialog，跳过 ACK：callId = {}", callId);
                return;
            }
            long cseq = ((CSeqHeader) evt.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber();
            Request ack = dialog.createAck(cseq);
            dialog.sendAck(ack);
            log.info("发送 ACK 响应（dialog-aware）：callId = {}, dialogState = {}", callId, dialog.getState());
        } catch (Exception e) {
            log.error("ACK 处理异常：callId = {}", callId, e);
        }
    }
}
