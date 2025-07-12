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
 *
 * @author luna
 * @date 2023/10/18
 */
@Component
@Getter
@Setter
@Slf4j
public class ClientInfoRequestProcessor extends SipRequestProcessorAbstract {

    public static final String METHOD = "INFO";

    private String method = METHOD;

    @Autowired
    private InfoProcessorClient infoProcessorClient;

    /**
     * 收到Info请求 处理
     *
     * @param evt
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

            // 调用业务处理器
            infoProcessorClient.receiveInfo(userId, content);

            // 发送200 OK响应
            ResponseCmd.doResponseCmd(Response.OK, evt);

        } catch (Exception e) {
            log.error("处理INFO请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.doResponseCmd(Response.SERVER_INTERNAL_ERROR, e.getMessage(), evt);
        }
    }
}
