package io.github.lunasaw.gbproxy.server.transmit.request.notify;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandlerAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * @author luna
 */
@Data
@Component
public abstract class NotifyServerHandlerAbstract extends MessageHandlerAbstract {

    public ApplicationEventPublisher publisher;

    public ServerDeviceSupplier serverDeviceSupplier;

    public NotifyServerHandlerAbstract(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        this.publisher = publisher;
        this.serverDeviceSupplier = serverDeviceSupplier;
    }

    @Override
    public String getRootType() {
        return "Root";
    }

    public DeviceSession getDeviceSession(RequestEvent event) {
        SIPRequest sipRequest = (SIPRequest) event.getRequest();

        // 客户端发送的userId
        String userId = SipUtils.getUserIdFromFromHeader(sipRequest);
        // 服务端接收的userId
        // 服务端收到消息，fromHeader是服务端的userId
        String sipId = SipUtils.getUserIdFromToHeader(sipRequest);

        return new DeviceSession(userId, sipId);
    }

}
