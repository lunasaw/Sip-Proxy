package io.github.lunasaw.gbproxy.server.transimit.request.bye;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transimit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Server模块BYE请求处理器
 * 只负责SIP协议层面的处理，业务逻辑通过ServerByeProcessorHandler接口实现
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class ByeRequestProcessorServer extends ServerAbstractSipRequestProcessor {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    private ServerByeProcessorHandler serverByeProcessorHandler;

    /**
     * 处理BYE请求
     * 只负责SIP协议层面的处理，业务逻辑通过ServerByeProcessorHandler接口实现
     *
     * @param evt 请求事件
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 解析协议层面的信息
            String sipId = SipUtils.getUserIdFromToHeader(request);
            String userId = SipUtils.getUserIdFromFromHeader(request);

            log.debug("处理BYE请求：用户ID = {}, SIP ID = {}", userId, sipId);

            // 验证设备权限
            if (!serverByeProcessorHandler.validateDevicePermission(userId, sipId, evt)) {
                log.warn("BYE请求权限验证失败：用户ID = {}, SIP ID = {}", userId, sipId);
                serverByeProcessorHandler.handleByeError(userId, "权限验证失败", evt);
                return;
            }

            // 调用业务处理器
            serverByeProcessorHandler.handleByeRequest(userId, evt);

        } catch (Exception e) {
            log.error("处理BYE请求异常：evt = {}", evt, e);
            String userId = SipUtils.getUserIdFromFromHeader((SIPRequest) evt.getRequest());
            serverByeProcessorHandler.handleByeError(userId, e.getMessage(), evt);
        }
    }

}
