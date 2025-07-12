package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.event.message.SipMessageRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端SUBSCRIBE请求处理器
 * 负责处理客户端收到的SUBSCRIBE请求，专注于协议层面处理
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class ClientSubscribeRequestProcessor extends SipMessageRequestProcessorAbstract {

    public static final String       METHOD = "SUBSCRIBE";

    private String                   method = METHOD;

    @Autowired
    private SubscribeProcessorClient subscribeProcessorClient;

    /**
     * 收到SUBSCRIBE请求 处理
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

            log.debug("收到SUBSCRIBE请求: from={}, to={}", fromUserId, toUserId);

            // 调用消息处理框架
            doMessageHandForEvt(evt, null);

        } catch (Exception e) {
            log.error("处理SUBSCRIBE请求时发生异常: evt = {}", evt, e);
        }
    }
}
