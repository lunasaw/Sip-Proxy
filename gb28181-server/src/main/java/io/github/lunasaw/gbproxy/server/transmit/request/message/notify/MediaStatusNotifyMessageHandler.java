package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gbproxy.server.transmit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.gbproxy.server.transmit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class MediaStatusNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = "MediaStatus";

    private String cmdType = CMD_TYPE;

    public MediaStatusNotifyMessageHandler(ServerMessageProcessorHandler serverMessageProcessorHandler, ServerDeviceSupplier serverDeviceSupplier) {
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
            // 未注册的设备不做处理
            return;
        }

        MediaStatusNotify mediaStatusNotify = parseXml(MediaStatusNotify.class);

        serverMessageProcessorHandler.updateMediaStatus(mediaStatusNotify);
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
