package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import javax.sip.RequestEvent;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.event.message.SipMessageRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端 SUBSCRIBE 请求处理器，负责接收并分发平台下发的 SUBSCRIBE 请求。
 */
@Component("clientSubscribeRequestProcessor")
@Getter
@Setter
@Slf4j
public class SubscribeRequestProcessor extends SipMessageRequestProcessorAbstract {

    public static final String       METHOD = "SUBSCRIBE";

    private String                   method = METHOD;

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

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
            FromDevice clientFromDevice = clientDeviceSupplier.getClientFromDevice();
            // 比较 toUserId d和clientFromDevice的userId是否一致 表示收到的消息就是要处理的消息
            if (!toUserId.equals(clientFromDevice.getUserId())) {
                log.warn("SUBSCRIBE请求的目标用户ID与客户端不匹配: from={}, to={}, clientFromDevice={}", fromUserId, toUserId, clientFromDevice);
                return; // 忽略不匹配的请求
            }
            // 调用消息处理框架
            doMessageHandForEvt(evt, clientFromDevice);

        } catch (Exception e) {
            log.error("处理SUBSCRIBE请求时发生异常: evt = {}", evt, e);
        }
    }
}
