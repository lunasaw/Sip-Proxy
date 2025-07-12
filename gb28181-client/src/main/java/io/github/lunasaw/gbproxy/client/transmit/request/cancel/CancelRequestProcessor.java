package io.github.lunasaw.gbproxy.client.transmit.request.cancel;

import javax.sip.RequestEvent;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户端CANCEL请求处理器
 * 负责处理客户端收到的CANCEL请求，专注于协议层面处理
 *
 * @author luna
 */
@Component
@Getter
@Setter
@Slf4j
public class CancelRequestProcessor extends SipRequestProcessorAbstract {

    private final String METHOD = "CANCEL";

    private String method = METHOD;

    /**
     * 处理CANCEL请求
     *
     * @param evt 事件
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 协议层面处理：解析SIP消息
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);
            String callId = SipUtils.getCallId(request);

            log.debug("收到CANCEL请求: from={}, to={}, callId={}", fromUserId, toUserId, callId);

            // TODO: 实现CANCEL请求的业务逻辑
            // 优先级99 Cancel Request消息实现，此消息一般为级联消息，上级给下级发送请求取消指令

        } catch (Exception e) {
            log.error("处理CANCEL请求时发生异常: evt = {}", evt, e);
        }
    }
}
