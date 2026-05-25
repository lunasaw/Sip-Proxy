package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 客户端 SUBSCRIBE 处理器抽象基类。
 *
 * <p>v1.5.0 改造：删除 {@code SubscribeRequestHandler} 字段与构造器入参，handler 直接维护内部
 * SubscribeRegistry（在调用 send 200 OK 的同时把 SubscribeInfo 写入注册表），并发布
 * {@link io.github.lunasaw.gbproxy.client.eventbus.event.ClientSubscribeEvent}。
 * 业务接入由 {@code SubscribeListener} 取代旧 {@code SubscribeRequestHandler} 接口。
 *
 * @author luna
 */
@Component
public abstract class SubscribeHandlerAbstract extends MessageHandlerAbstract {

    @Autowired
    protected ClientDeviceSupplier clientDeviceSupplier;

    @Override
    public String getRootType() {
        return SubscribeRequestProcessor.METHOD + "Root";
    }

    public DeviceSession getDeviceSession(RequestEvent event) {
        SIPRequest sipRequest = (SIPRequest) event.getRequest();

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
