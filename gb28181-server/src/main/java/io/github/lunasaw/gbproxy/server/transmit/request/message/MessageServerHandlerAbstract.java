package io.github.lunasaw.gbproxy.server.transmit.request.message;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 服务端 MESSAGE 请求处理器抽象基类，提供设备会话获取和前置校验能力。
 *
 * @author luna
 */
@Data
@Component
public abstract class MessageServerHandlerAbstract extends MessageHandlerAbstract implements InitializingBean {

    public ApplicationEventPublisher publisher;

    public ServerDeviceSupplier serverDeviceSupplier;


    public MessageServerHandlerAbstract(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        this.publisher = publisher;
        this.serverDeviceSupplier = serverDeviceSupplier;
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
     * 从 SIP 请求中提取设备会话信息（fromHeader=设备ID，toHeader=平台ID）。
     *
     * @param event SIP 请求事件
     * @return 设备会话信息
     */
    public DeviceSession getDeviceSession(RequestEvent event) {
        SIPRequest sipRequest = (SIPRequest) event.getRequest();

        // 客户端发送的userId
        String userId = SipUtils.getUserIdFromFromHeader(sipRequest);
        // 服务端接收的userId
        // 服务端收到消息，fromHeader是服务端的userId
        String sipId = SipUtils.getUserIdFromToHeader(sipRequest);

        return new DeviceSession(userId, sipId);
    }

    /**
     * 前置校验：验证设备身份并确认设备已注册。
     *
     * @param event SIP 请求事件
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean preCheck(RequestEvent event) {
        if (!serverDeviceSupplier.checkDevice(event)) {
            return false;
        }
        DeviceSession deviceSession = getDeviceSession(event);
        String userId = deviceSession.getUserId();
        // 设备查询
        Device toDevice = serverDeviceSupplier.getDevice(userId);
        if (toDevice == null) {
            // 未注册的设备不做处理
            return false;
        }

        return true;
    }

}
