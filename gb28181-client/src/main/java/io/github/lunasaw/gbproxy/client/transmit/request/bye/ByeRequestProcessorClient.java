package io.github.lunasaw.gbproxy.client.transmit.request.bye;

import io.github.lunasaw.gbproxy.client.transmit.response.bye.ClientByeProcessorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户端BYE请求处理器
 * 负责处理客户端收到的BYE请求，专注于协议层面处理
 *
 * @author luna
 */
@Component("byeRequestProcessorClient")
@Getter
@Setter
@Slf4j
public class ByeRequestProcessorClient extends SipRequestProcessorAbstract {

    public static final String METHOD = "BYE";

    private String method = METHOD;

    @Autowired
    @Lazy
    private ClientByeProcessorHandler clientByeProcessorHandler;

    /**
     * 收到Bye请求 处理
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
            String callId = SipUtils.getCallId(request);

            log.debug("收到BYE请求: from={}, to={}, callId={}", fromUserId, toUserId, callId);

            // 发送200 OK响应
            ResponseCmd.doResponseCmd(Response.OK, evt);

            // 检查对话状态
            Dialog dialog = evt.getDialog();
            if (dialog != null) {
                // 调用业务处理器关闭流
                clientByeProcessorHandler.closeStream(dialog.getCallId().getCallId());
            }

        } catch (Exception e) {
            log.error("处理BYE请求时发生异常: evt = {}", evt, e);
            // 发送500错误响应
            ResponseCmd.doResponseCmd(Response.SERVER_INTERNAL_ERROR, evt);
        }
    }
}
