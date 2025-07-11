package io.github.lunasaw.gbproxy.client.transmit.response.register;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.client.config.Gb28181ClientProperties;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
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
 * description 发起后 Register 的响应处理器
 * 业务逻辑直接继承该类，实现方法即可
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component
public class RegisterResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "REGISTER";

    public String method = METHOD;

    @Autowired
    private RegisterProcessorClient registerProcessorClient;

    @Autowired
    private DefaultDeviceSupplier deviceSupplier;

    @Autowired
    private Gb28181ClientProperties gb28181ClientProperties;

    /**
     * 处理Register响应
     *
     * @param evt 事件
     */
    @Override
    public void process(ResponseEvent evt) {
        SIPResponse response = (SIPResponse) evt.getResponse();
        String callId = response.getCallIdHeader().getCallId();
        if (StringUtils.isBlank(callId)) {
            return;
        }

        ResponseEventExt eventExt = (ResponseEventExt) evt;
        if (response.getStatusCode() == Response.UNAUTHORIZED) {
            try {
                responseUnAuthorized(eventExt);
            } catch (SdpParseException e) {
                log.error("process responseUnAuthorized error::evt = {} ", evt, e);
            }
        } else if (response.getStatusCode() == Response.OK) {
            String toUserId = SipUtils.getUserIdFromToHeader(response);
            registerProcessorClient.registerSuccess(toUserId);
        }
    }

    public void responseUnAuthorized(ResponseEventExt evt) throws SdpParseException {
        // 成功响应
        SIPResponse response = (SIPResponse) evt.getResponse();

        String toUserId = SipUtils.getUserIdFromToHeader(response);

        CallIdHeader callIdHeader = response.getCallIdHeader();
        int registerExpires = gb28181ClientProperties.getRegisterExpires();
        String clientId = gb28181ClientProperties.getClientId();
        FromDevice fromDevice = (FromDevice) deviceSupplier.getDevice(clientId);
        ToDevice toDevice = (ToDevice) deviceSupplier.getDevice(toUserId);
        if (fromDevice == null || toDevice == null) {
            return;
        }

        WWWAuthenticateHeader www = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);

        Request registerRequestWithAuth = SipRequestBuilderFactory.createRegisterRequestWithAuth(fromDevice, toDevice, callIdHeader.getCallId(), registerExpires, www);

        // 发送二次请求
        SipSender.transmitRequest(fromDevice.getIp(), registerRequestWithAuth);
    }
}
