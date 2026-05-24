package io.github.lunasaw.gbproxy.client.transmit.request.message;

import javax.sip.RequestEvent;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端消息处理器抽象基类。
 *
 * <p>v1.5.0 改造：删除 {@code MessageRequestHandler} 字段与构造器入参，handler 改为发布
 * {@link io.github.lunasaw.gbproxy.client.eventbus.event.ClientQueryEvent} 等 L1 协议事件，
 * 业务接入由 {@code QueryListener} / {@code ControlListener} / ... 取代旧 {@code MessageRequestHandler} 接口。
 *
 * @author luna
 */
@Slf4j
@Getter
@Component
public abstract class MessageClientHandlerAbstract extends MessageHandlerAbstract {

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

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

        // 设置FromDevice - 客户端响应时使用
        FromDevice clientFromDevice = clientDeviceSupplier.getClientFromDevice();
        if (clientFromDevice == null) {
            // log 这里获取不到客户端发送设备，表示当前配置未启动客户端，需要抛出异常
            throw new RuntimeException("获取不到客户端发送设备，表示当前配置未启动客户端");
        }
        deviceSession.setFromDevice(clientFromDevice);

        // 设置ToDevice - 响应的目标设备（通常是服务端）
        Device device = clientDeviceSupplier.getDevice(sipId);
        if (device == null) {
            // log 这里获取不到目标设备，表示当前配置未启动服务端，需要抛出异常
            throw new RuntimeException("获取不到目标设备，表示当前配置未启动服务端");
        }
        deviceSession.setToDevice(clientDeviceSupplier.getToDevice(device));

        return deviceSession;
    }
}
