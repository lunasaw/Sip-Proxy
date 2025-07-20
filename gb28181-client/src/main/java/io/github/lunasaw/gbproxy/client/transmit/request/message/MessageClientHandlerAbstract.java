package io.github.lunasaw.gbproxy.client.transmit.request.message;

import javax.sip.RequestEvent;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
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
@ConditionalOnBean(MessageRequestHandler.class)
public abstract class MessageClientHandlerAbstract extends MessageHandlerAbstract {

    @Autowired
    public MessageRequestHandler messageRequestHandler;

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    public MessageClientHandlerAbstract(@Lazy MessageRequestHandler messageRequestHandler) {
        this.messageRequestHandler = messageRequestHandler;
    }

    @Override
    public String getRootType() {
        return "Root";
    }


    @Override
    public String getMethod() {
        return "MESSAGE";
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

        DeviceSession deviceSession = new DeviceSession(userId, sipId);
        FromDevice clientFromDevice = clientDeviceSupplier.getClientFromDevice();
        if (clientFromDevice != null && clientFromDevice.getUserId().equals(userId)) {
            // 如果客户端的fromDevice和请求中的userId一致，说明是自己的设备
            deviceSession.setFromDevice(clientFromDevice);
        }
        Device device = clientDeviceSupplier.getDevice(sipId);
        if (device != null) {
            ToDevice toDevice = clientDeviceSupplier.getToDevice(device);
            deviceSession.setToDevice(toDevice);
        }
        return deviceSession;
    }
}
