package io.github.lunasaw.gbproxy.client.transmit.response.register;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.client.config.ClientProperties;
import io.github.lunasaw.gbproxy.client.transmit.response.ClientAbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.service.DefaultDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Register响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过RegisterProcessorHandler接口实现
 * 这个是客户端发起的REGISTER后，服务端回复的REGISTER响应处理器
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("clientRegisterResponseProcessor")
public class ClientRegisterResponseProcessor extends ClientAbstractSipResponseProcessor {

    public static final String METHOD = "REGISTER";

    private String method = METHOD;

    @Autowired
    private ClientDeviceSupplier deviceSupplier;

    @Autowired
    private ClientProperties clientProperties;

    @Autowired
    private RegisterProcessorHandler registerProcessorHandler;

    /**
     * 处理Register响应
     *
     * @param evt 事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            if (StringUtils.isBlank(callId)) {
                log.warn("Register响应处理失败：callId为空");
                return;
            }

            String toUserId = SipUtils.getUserIdFromToHeader(response);
            int statusCode = response.getStatusCode();

            if (statusCode == Response.UNAUTHORIZED) {
                handleUnauthorizedResponse(evt, toUserId, callId);
            } else if (statusCode == Response.OK) {
                registerProcessorHandler.registerSuccess(toUserId);
                log.info("Register成功：toUserId = {}", toUserId);
            } else {
                registerProcessorHandler.handleRegisterFailure(toUserId, statusCode);
                log.warn("Register失败：toUserId = {}, statusCode = {}", toUserId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理Register响应异常：evt = {}", evt, e);
        }
    }

    /**
     * 处理未授权响应
     *
     * @param evt      响应事件
     * @param toUserId 目标用户ID
     * @param callId   呼叫ID
     */
    private void handleUnauthorizedResponse(ResponseEvent evt, String toUserId, String callId) {
        try {
            ResponseEventExt eventExt = (ResponseEventExt) evt;
            SIPResponse response = (SIPResponse) evt.getResponse();

            // 调用业务处理器
            registerProcessorHandler.handleUnauthorized(evt, toUserId, callId);

            // 协议层面的重新认证处理
            processReAuthentication(eventExt, toUserId, callId);

        } catch (Exception e) {
            log.error("处理未授权响应异常：toUserId = {}, callId = {}", toUserId, callId, e);
        }
    }

    /**
     * 处理重新认证
     *
     * @param evt      响应事件
     * @param toUserId 目标用户ID
     * @param callId   呼叫ID
     */
    private void processReAuthentication(ResponseEventExt evt, String toUserId, String callId) throws SdpParseException {
        SIPResponse response = (SIPResponse) evt.getResponse();
        CallIdHeader callIdHeader = response.getCallIdHeader();

        String clientId = clientProperties.getAuth().getDeviceId();
        FromDevice fromDevice = (FromDevice) deviceSupplier.getDevice(clientId);
        ToDevice toDevice = (ToDevice) deviceSupplier.getDevice(toUserId);

        if (fromDevice == null || toDevice == null) {
            log.error("设备信息获取失败：fromDevice = {}, toDevice = {}", fromDevice, toDevice);
            return;
        }

        WWWAuthenticateHeader www = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
        if (www == null) {
            log.error("未找到WWW-Authenticate头");
            return;
        }

        Integer expire = registerProcessorHandler.getExpire(toUserId);
        Request registerRequestWithAuth = SipRequestBuilderFactory.createRegisterRequestWithAuth(
                fromDevice, toDevice, callIdHeader.getCallId(), expire, www);

        // 发送二次请求
        SipSender.transmitRequest(fromDevice.getIp(), registerRequestWithAuth);
        log.info("发送重新认证请求：toUserId = {}, callId = {}", toUserId, callId);
    }
}
