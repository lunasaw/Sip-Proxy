package io.github.lunasaw.gbproxy.server.transimit.request.info;

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
 * Server模块INFO请求处理器
 * 只负责SIP协议层面的处理，业务逻辑通过ServerInfoProcessorHandler接口实现
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class ServerInfoRequestProcessor extends ServerAbstractSipRequestProcessor {

    public static final String METHOD = "INFO";

    private String method = METHOD;

    @Autowired
    private ServerInfoProcessorHandler serverInfoProcessorHandler;

    /**
     * 处理INFO请求
     * 只负责SIP协议层面的处理，业务逻辑通过ServerInfoProcessorHandler接口实现
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

            log.debug("处理INFO请求：用户ID = {}, SIP ID = {}", userId, sipId);

            // 验证设备权限
            if (!serverInfoProcessorHandler.validateDevicePermission(userId, sipId, evt)) {
                log.warn("INFO请求权限验证失败：用户ID = {}, SIP ID = {}", userId, sipId);
                serverInfoProcessorHandler.handleInfoError(userId, "权限验证失败", evt);
                return;
            }

            // 获取请求内容
            String content = "";
            if (evt.getRequest().getRawContent() != null) {
                content = new String(evt.getRequest().getRawContent());
            }

            // 调用业务处理器
            serverInfoProcessorHandler.handleInfoRequest(userId, content, evt);

        } catch (Exception e) {
            log.error("处理INFO请求异常：evt = {}", evt, e);
            String userId = SipUtils.getUserIdFromFromHeader((SIPRequest) evt.getRequest());
            serverInfoProcessorHandler.handleInfoError(userId, e.getMessage(), evt);
        }
    }

}
