package io.github.lunasaw.gbproxy.server.transmit.response.invite;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ResponseEvent;
import javax.sip.address.SipURI;

/**
 * 默认INVITE响应处理器业务实现
 * 负责具体的业务逻辑处理
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnMissingBean(InviteResponseProcessorHandler.class)
public class DefaultInviteResponseProcessorHandler implements InviteResponseProcessorHandler {

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Override
    public void handleTryingResponse(ResponseEvent evt, String callId) {
        log.debug("处理INVITE Trying响应：callId = {}", callId);
        // 默认实现为空，由业务方根据需要实现
    }

    @Override
    public void handleOkResponse(ResponseEvent evt, String callId) {
        log.info("处理INVITE OK响应：callId = {}", callId);
        // 默认实现为空，由业务方根据需要实现
    }

    @Override
    public void processOkResponse(ResponseEventExt evt, String callId) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            FromDevice fromDevice = (FromDevice) serverDeviceSupplier.getServerFromDevice();

            // 检查响应是否包含SDP内容
            byte[] rawContent = response.getRawContent();
            if (rawContent == null || rawContent.length == 0) {
                log.debug("INVITE OK响应不包含SDP内容，跳过处理：callId = {}", callId);
                return;
            }

            String contentString = new String(rawContent);
            SdpSessionDescription gb28181Sdp = SipUtils.parseSdp(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();

            SipURI requestUri = SipRequestUtils.createSipUri(sdp.getOrigin().getUsername(),
                    evt.getRemoteIpAddress() + ":" + evt.getRemotePort());

            // 回复ack
            ServerCommandSender.deviceAck(fromDevice, requestUri, response);
            log.info("发送ACK响应：requestUri = {}, callId = {}", requestUri, callId);
        } catch (SdpParseException e) {
            log.error("处理INVITE OK响应异常：callId = {}", callId, e);
        } catch (Exception e) {
            log.error("处理INVITE响应异常：evt = {}", evt, e);
        }
    }

    @Override
    public void handleFailureResponse(ResponseEvent evt, String callId, int statusCode) {
        log.warn("处理INVITE失败响应：callId = {}, statusCode = {}", callId, statusCode);
        // 默认实现为空，由业务方根据需要实现
    }
}