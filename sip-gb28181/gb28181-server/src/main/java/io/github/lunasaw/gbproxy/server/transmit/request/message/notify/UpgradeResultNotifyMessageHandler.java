package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerNotifyEvent;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
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
 * GB28181-2022 §9.13 / A.2.5.9 设备软件升级结果通知 处理器
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class UpgradeResultNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.DEVICE_UPGRADE_RESULT.getType();

    private String cmdType = CMD_TYPE;

    public UpgradeResultNotifyMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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
        Device toDevice = serverDeviceSupplier.getDevice(userId);
        if (toDevice == null) {
            return;
        }

        UpgradeResultNotify notify = parseXml(UpgradeResultNotify.class);
        if (notify == null) {
            log.warn("解析设备软件升级结果通知失败: deviceId={}", userId);
            return;
        }

        publisher.publishEvent(new ServerNotifyEvent(this, notify.getDeviceId(), notify));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
