package io.github.lunasaw.gbproxy.client.transmit.request.info;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端INFO请求处理器
 * 负责处理客户端收到的INFO请求，专注于协议层面处理
 * 按照SIP处理器业务逻辑分离规范，只负责SIP协议层面的处理，不包含业务逻辑
 *
 * @author luna
 * @date 2023/10/18
 */
@Component("clientInfoRequestProcessor")
@Getter
@Setter
@Slf4j
public class InfoRequestProcessor extends SipRequestProcessorAbstract {

    public static final String METHOD = "INFO";

    private String method = METHOD;

    @Autowired
    private InfoRequestHandler infoRequestHandler;

    /**
     * 收到Info请求 处理
     * 专注于协议层面处理：消息解析、参数提取、调用业务处理器、响应构建
     *
     * @param evt INFO请求事件
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 协议层面处理：解析SIP消息
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);

            log.debug("收到INFO请求: from={}, to={}", fromUserId, toUserId);

            // 在客户端看来，收到请求时fromHeader是服务端，toHeader是客户端
            String userId = toUserId;
            String content = new String(request.getRawContent());

            // 调用业务处理器接口，不包含具体业务逻辑
            infoRequestHandler.receiveInfo(userId, content);

            // 发送200 OK响应
            ResponseCmd.doResponseCmd(Response.OK, evt);

        } catch (Exception e) {
            log.error("处理INFO请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.doResponseCmd(Response.SERVER_INTERNAL_ERROR, e.getMessage(), evt);
        }
    }
}
