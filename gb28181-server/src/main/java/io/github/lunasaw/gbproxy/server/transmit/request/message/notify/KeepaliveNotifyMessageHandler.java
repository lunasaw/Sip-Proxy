package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerLifecycleEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerNotifyEvent;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gbproxy.server.transmit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

/**
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class KeepaliveNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = "Keepalive";

    public KeepaliveNotifyMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        super(publisher, serverDeviceSupplier);
    }

    @Override
    public String getRootType() {
        return NOTIFY;
    }


    @Override
    public boolean needResponseAck() {
        return false;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceSession deviceSession = getDeviceSession(event);

        String userId = deviceSession.getUserId();
        Device device = serverDeviceSupplier.getDevice(userId);
        if (device == null) {
            log.warn("device not register, userId: {}", userId);
            ResponseCmd.sendResponse(Response.NOT_FOUND, event, event.getServerTransaction());
            return;
        }
        DeviceKeepLiveNotify deviceKeepLiveNotify = parseXml(DeviceKeepLiveNotify.class);
        publisher.publishEvent(new ServerNotifyEvent(this, deviceKeepLiveNotify.getDeviceId(), deviceKeepLiveNotify));

        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest((SIPRequest) event.getRequest());
        publisher.publishEvent(ServerLifecycleEvent.remoteAddressChanged(this, userId, remoteAddressInfo));

        ResponseCmd.sendResponse(Response.OK, "OK", event, event.getServerTransaction());
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }
}
