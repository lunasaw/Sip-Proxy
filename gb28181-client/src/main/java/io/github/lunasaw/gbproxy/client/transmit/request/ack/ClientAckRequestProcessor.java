package io.github.lunasaw.gbproxy.client.transmit.request.ack;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.event.SipSubscribe;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端ACK请求处理器
 * 负责处理客户端收到的ACK请求，专注于协议层面处理
 *
 * @author weidian
 */
@Component("clientAckRequestProcessor")
@Getter
@Setter
@Slf4j
public class ClientAckRequestProcessor extends SipRequestProcessorAbstract {

    private final String METHOD = "ACK";

    private String method = METHOD;

    @Autowired
    private AckRequestHandler ackRequestHandler;

    /**
     * 处理 ACK请求
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

            log.debug("收到ACK请求: from={}, to={}", fromUserId, toUserId);

            // 检查对话状态
            Dialog dialog = evt.getDialog();
            if (dialog == null) {
                log.warn("ACK请求没有关联的对话");
                return;
            }

            if (dialog.getState() == DialogState.CONFIRMED) {
                // 发布ACK事件
                SipSubscribe.publishAckEvent(evt);

                // 调用业务处理器
                ackRequestHandler.processAck(evt);
            }

        } catch (Exception e) {
            log.error("处理ACK请求时发生异常: evt = {}", evt, e);
        }
    }
}
