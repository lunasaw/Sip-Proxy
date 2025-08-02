package io.github.lunasaw.gbproxy.server.transmit.response.subscribe;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;

/**
 * SUBSCRIBE响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过SubscribeResponseProcessorHandler接口实现
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("serverSubscribeResponseProcessor")
public class SubscribeResponseProcessor extends ServerAbstractSipResponseProcessor {

    public static final String METHOD = "SUBSCRIBE";

    private String method = METHOD;

    @Autowired
    private SubscribeResponseProcessorHandler subscribeResponseProcessorHandler;

    /**
     * 处理SUBSCRIBE响应
     *
     * @param evt 响应事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            int statusCode = response.getStatusCode();

            if (callId == null) {
                log.warn("SUBSCRIBE响应处理失败：callId为空");
                return;
            }

            if (statusCode == Response.OK) {
                DeviceSubscribe deviceSubscribe = SipUtils.parseResponse(evt, DeviceSubscribe.class);
                subscribeResponseProcessorHandler.responseSubscribe(deviceSubscribe);
                log.info("处理SUBSCRIBE成功响应：callId = {}", callId);
            } else {
                subscribeResponseProcessorHandler.handleSubscribeFailure(evt, callId, statusCode);
                log.warn("处理SUBSCRIBE失败响应：callId = {}, statusCode = {}", callId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理SUBSCRIBE响应异常：evt = {}", evt, e);
        }
    }
}
