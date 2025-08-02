package io.github.lunasaw.gbproxy.server.transmit.response.invite;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.server.transmit.response.ServerAbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

/**
 * INVITE响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过InviteResponseProcessorHandler接口实现
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
    private InviteResponseProcessorHandler inviteResponseProcessorHandler;

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

            // 协议层面的基础验证
            if (callId == null) {
                log.warn("INVITE响应处理失败：callId为空");
                return;
            }

            // 根据状态码分发到不同的业务处理器方法
            if (statusCode == Response.TRYING) {
                inviteResponseProcessorHandler.handleTryingResponse(evt, callId);
                log.debug("处理INVITE Trying响应：callId = {}", callId);
            } else if (statusCode == Response.OK) {
                // 调用业务处理器处理OK响应
                inviteResponseProcessorHandler.handleOkResponse(evt, callId);
                // 调用业务处理器处理OK响应的协议层面逻辑
                inviteResponseProcessorHandler.processOkResponse((ResponseEventExt) evt, callId);
                log.info("处理INVITE OK响应：callId = {}", callId);
            } else {
                inviteResponseProcessorHandler.handleFailureResponse(evt, callId, statusCode);
                log.warn("处理INVITE失败响应：callId = {}, statusCode = {}", callId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理INVITE响应异常：evt = {}", evt, e);
        }
    }
}
