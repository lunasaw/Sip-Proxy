package io.github.lunasaw.gbproxy.server.transmit.response.invite;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerSessionEvent;
import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ResponseEvent;
import javax.sip.address.SipURI;
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

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Autowired
    private ServerCommandSender serverCommandSender;

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
     * 协议层 ACK 回复：解析 SDP，构造 SipURI，发送 ACK。
     */
    private void sendAck(ResponseEventExt evt, String callId) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            FromDevice fromDevice = (FromDevice) serverDeviceSupplier.getServerFromDevice();

            byte[] rawContent = response.getRawContent();
            if (rawContent == null || rawContent.length == 0) {
                log.debug("INVITE OK响应不包含SDP内容，跳过ACK：callId = {}", callId);
                return;
            }

            String contentString = new String(rawContent);
            SdpSessionDescription gb28181Sdp = SipUtils.parseSdp(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();

            SipURI requestUri = SipRequestUtils.createSipUri(sdp.getOrigin().getUsername(),
                    evt.getRemoteIpAddress() + ":" + evt.getRemotePort());

            serverCommandSender.deviceAckBySipUri(fromDevice, requestUri, response);
            log.info("发送ACK响应：requestUri = {}, callId = {}", requestUri, callId);
        } catch (SdpParseException e) {
            log.error("ACK 处理 SDP 解析异常：callId = {}", callId, e);
        } catch (Exception e) {
            log.error("ACK 处理异常：callId = {}", callId, e);
        }
    }
}
