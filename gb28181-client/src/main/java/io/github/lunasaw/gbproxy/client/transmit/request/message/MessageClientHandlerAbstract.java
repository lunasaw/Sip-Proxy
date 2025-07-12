package io.github.lunasaw.gbproxy.client.transmit.request.message;

import javax.sip.RequestEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;

/**
 * 客户端消息处理器抽象基类
 * 提供客户端消息处理的通用功能
 *
 * @author luna
 */
@Getter
@Component
@ConditionalOnBean(MessageProcessorClient.class)
public abstract class MessageClientHandlerAbstract extends MessageHandlerAbstract {

    @Autowired
    public MessageProcessorClient messageProcessorClient;

    public MessageClientHandlerAbstract(@Lazy MessageProcessorClient messageProcessorClient) {
        this.messageProcessorClient = messageProcessorClient;
    }

    @Override
    public String getRootType() {
        return "Root";
    }

    /**
     * 获取设备会话信息
     * 客户端收到消息时，fromHeader是服务端，toHeader是客户端
     *
     * @param event 请求事件
     * @return DeviceSession 设备会话信息
     */
    public DeviceSession getDeviceSession(RequestEvent event) {
        SIPRequest sipRequest = (SIPRequest)event.getRequest();

        // 特别注意：客户端收到消息，fromHeader是服务端，toHeader是客户端
        String userId = SipUtils.getUserIdFromToHeader(sipRequest);
        String sipId = SipUtils.getUserIdFromFromHeader(sipRequest);

        return new DeviceSession(userId, sipId);
    }
}
