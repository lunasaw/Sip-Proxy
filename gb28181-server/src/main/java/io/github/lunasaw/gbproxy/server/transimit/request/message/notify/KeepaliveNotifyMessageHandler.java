package io.github.lunasaw.gbproxy.server.transimit.request.message.notify;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gbproxy.server.transimit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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

    public KeepaliveNotifyMessageHandler(ServerMessageProcessorHandler serverMessageProcessorHandler, ServerDeviceSupplier serverDeviceSupplier) {
        super(serverMessageProcessorHandler, serverDeviceSupplier);
    }

    @Override
    public String getRootType() {
        return NOTIFY;
    }


    @Override
    public void handForEvt(RequestEvent event) {
        DeviceSession deviceSession = getDeviceSession(event);

        String userId = deviceSession.getUserId();
        // 设备查询
        Device device = serverDeviceSupplier.getDevice(userId);
        if (device == null) {
            // 未注册的设备回复失败
            log.warn("device not register, userId: {}", userId);
            ResponseCmd.doResponseCmd(Response.NOT_FOUND, event);
            return;
        }
        DeviceKeepLiveNotify deviceKeepLiveNotify = parseXml(DeviceKeepLiveNotify.class);
        serverMessageProcessorHandler.keepLiveDevice(deviceKeepLiveNotify);

        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest((SIPRequest) event.getRequest());
        serverMessageProcessorHandler.updateRemoteAddress(userId, remoteAddressInfo);

        // 发送200 OK响应
        responseAck(event);
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }
}
