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
 * 发起INVITE响应
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component
public class InviteResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    private static final String           METHOD = "INVITE";

    private String                        method = METHOD;

    @Autowired
    private InviteResponseProcessorServer inviteResponseProcessorServer;

    @Autowired
    private SipUserGenerateServer         sipUserGenerate;

    /**
     * 处理invite响应
     *
     * @param evt 响应消息
     * @throws ParseException
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse)evt.getResponse();
            int statusCode = response.getStatusCode();
            if (statusCode == Response.TRYING) {
                inviteResponseProcessorServer.responseTrying();
            }

            if (statusCode == Response.OK) {
                ResponseEventExt event = (ResponseEventExt)evt;
                responseAck(event);
            }
        } catch (Exception e) {
            log.info("[点播回复ACK]，异常：", e);
        }
    }

    public void responseAck(ResponseEventExt evt) throws SdpParseException {
        // 成功响应
        SIPResponse response = (SIPResponse)evt.getResponse();

        FromDevice fromDevice = (FromDevice)sipUserGenerate.getFromDevice();

        String contentString = new String(response.getRawContent());
        SdpSessionDescription gb28181Sdp = SipUtils.parseSdp(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();

        SipURI requestUri = SipRequestUtils.createSipUri(sdp.getOrigin().getUsername(), evt.getRemoteIpAddress() + ":" + evt.getRemotePort());

        // 回复ack
        ServerSendCmd.deviceAck(fromDevice, requestUri, response);
    }

}
