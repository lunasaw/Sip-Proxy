package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAlarmEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 处理设备告警信息
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class AlarmNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = "Alarm";

    public AlarmNotifyMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        super(publisher, serverDeviceSupplier);
    }

    @Override
    public String getRootType() {
        return NOTIFY;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        if (!serverDeviceSupplier.checkDevice(event)) {
            return;
        }
        DeviceSession deviceSession = getDeviceSession(event);
        String userId = deviceSession.getUserId();
        // 设备查询
        Device toDevice = serverDeviceSupplier.getDevice(userId);
        if (toDevice == null) {
            // 未注册的设备不做处理
            return;
        }

        DeviceAlarmNotify deviceAlarmNotify = parseXml(DeviceAlarmNotify.class);

        publisher.publishEvent(new DeviceAlarmEvent(this, deviceAlarmNotify.deviceId, deviceAlarmNotify));
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }
}
