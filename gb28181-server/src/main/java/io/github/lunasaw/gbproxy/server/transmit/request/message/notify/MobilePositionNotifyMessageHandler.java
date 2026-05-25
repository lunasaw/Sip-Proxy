package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerNotifyEvent;
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
 * GB28181-2022 §A.2.5.6 移动设备位置数据通知 处理器
 * (rootType=Notify, cmdType=MobilePosition, method=MESSAGE)。
 *
 * <p>v1.5.6 起补全：此前 v1.5.5 矩阵审计发现 server 端无对应 handler，设备 GPS 位置上报
 * 直接��� dispatcher 静默丢弃。本 handler 解析 {@link MobilePositionNotify} 并发布
 * {@link ServerNotifyEvent}，由 {@code ServerListenerAdapter} 路由到
 * {@code DeviceNotifyListener.onMobilePositionNotify}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class MobilePositionNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.MOBILE_POSITION.getType();

    private String cmdType = CMD_TYPE;

    public MobilePositionNotifyMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        MobilePositionNotify notify = parseXml(MobilePositionNotify.class);
        if (notify == null) {
            log.warn("解析移动位置通知失败: deviceId={}", userId);
            return;
        }

        publisher.publishEvent(new ServerNotifyEvent(this, notify.getDeviceId(), notify));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
