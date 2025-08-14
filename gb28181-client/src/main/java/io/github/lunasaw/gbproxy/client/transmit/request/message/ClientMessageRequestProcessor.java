package io.github.lunasaw.gbproxy.client.transmit.request.message;

import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.transmit.event.message.SipMessageRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端MESSAGE请求处理器
 * 负责处理客户端收到的MESSAGE请求，专注于协议层面处理
 *
 * @author luna
 */
@Component("clientMessageRequestProcessor")
@Getter
@Setter
@Slf4j
public class ClientMessageRequestProcessor extends SipMessageRequestProcessorAbstract {

    public static final String     METHOD = "MESSAGE";

    private String                 method = METHOD;

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            // 协议层面处理：解析SIP消息
            String fromUserId = SipUtils.getUserIdFromFromHeader(request);
            String toUserId = SipUtils.getUserIdFromToHeader(request);

            log.info("收到MESSAGE请求: from={}, to={}", fromUserId, toUserId);
            // 获取FromDevice
            FromDevice clientFromDevice = clientDeviceSupplier.getClientFromDevice();
            // 比较 toUserId 和 clientFromDevice 的 userId 是否一致 表示收到的消息就是要处理的消息
            if (!toUserId.equals(clientFromDevice.getUserId())) {
                log.warn("MESSAGE请求的目标用户ID与客户端不匹配: from={}, to={}, clientFromDevice={}", fromUserId, toUserId, clientFromDevice);
                return; // 忽略不匹配的请求
            }
            // 调用消息处理框架
            doMessageHandForEvt(evt, clientFromDevice);

        } catch (Exception e) {
            log.error("处理MESSAGE请求时发生异常: evt = {}", evt, e);
        }
    }
}
