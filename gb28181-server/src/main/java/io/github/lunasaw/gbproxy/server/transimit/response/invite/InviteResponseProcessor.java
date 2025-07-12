package io.github.lunasaw.gbproxy.server.transimit.response.invite;

import java.text.ParseException;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ResponseEvent;
import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transimit.cmd.ServerSendCmd;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * INVITE响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过InviteResponseProcessorServer接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component
public class InviteResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "INVITE";

    private String method = METHOD;

    @Autowired
    private InviteResponseProcessorServer inviteResponseProcessorServer;

    @Autowired
    private SipUserGenerateServer sipUserGenerate;

    /**
     * 处理invite响应
     *
     * @param evt 响应消息
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse)evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId == null) {
                log.warn("INVITE响应处理失败：callId为空");
                return;
            }

            if (statusCode == Response.TRYING) {
                inviteResponseProcessorServer.responseTrying();
                log.debug("处理INVITE Trying响应：callId = {}", callId);
            } else if (statusCode == Response.OK) {
                inviteResponseProcessorServer.handleOkResponse(evt, callId);
                log.info("处理INVITE OK响应：callId = {}", callId);
                processOkResponse((ResponseEventExt)evt);
            } else {
                inviteResponseProcessorServer.handleFailureResponse(evt, callId, statusCode);
                log.warn("处理INVITE失败响应：callId = {}, statusCode = {}", callId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理INVITE响应异常：evt = {}", evt, e);
        }
    }

    /**
     * 处理OK响应
     *
     * @param evt 响应事件
     */
    private void processOkResponse(ResponseEventExt evt) throws SdpParseException {
        SIPResponse response = (SIPResponse)evt.getResponse();
        FromDevice fromDevice = (FromDevice)sipUserGenerate.getFromDevice();

        String contentString = new String(response.getRawContent());
        SdpSessionDescription gb28181Sdp = SipUtils.parseSdp(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();

        SipURI requestUri = SipRequestUtils.createSipUri(sdp.getOrigin().getUsername(),
            evt.getRemoteIpAddress() + ":" + evt.getRemotePort());

        // 回复ack
        ServerSendCmd.deviceAck(fromDevice, requestUri, response);
        log.info("发送ACK响应：requestUri = {}", requestUri);
    }
}
