package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * @author luna
 */
@Data
@Component
public abstract class SubscribeHandlerAbstract extends MessageHandlerAbstract {

    @Autowired
    @Lazy
    protected SubscribeRequestHandler subscribeRequestHandler;

    @Autowired
    protected ClientDeviceSupplier clientDeviceSupplier;

    public SubscribeHandlerAbstract(@Lazy SubscribeRequestHandler subscribeRequestHandler, ClientDeviceSupplier deviceSupplier) {
        this.subscribeRequestHandler = subscribeRequestHandler;
        this.clientDeviceSupplier = deviceSupplier;
    }

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
