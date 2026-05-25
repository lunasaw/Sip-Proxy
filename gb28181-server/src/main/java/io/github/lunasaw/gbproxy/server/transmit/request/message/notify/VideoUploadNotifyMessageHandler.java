package io.github.lunasaw.gbproxy.server.transmit.request.message.notify;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.VideoUploadNotify;
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
 * GB28181-2022 §A.2.5.8 设备实时视音频回传通知 处理器
 * (rootType=Notify, cmdType=VideoUploadNotify, method=MESSAGE)。
 *
 * <p>v1.5.6 起补全：执法记录仪、移动单警等设备开始/结束实时视音频回传时，向平台主动通知，
 * 平台据此做"是否启用回传媒体流"的会话调度。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class VideoUploadNotifyMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.VIDEO_UPLOAD_NOTIFY.getType();

    private String cmdType = CMD_TYPE;

    public VideoUploadNotifyMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        VideoUploadNotify notify = parseXml(VideoUploadNotify.class);
        if (notify == null) {
            log.warn("解析设备实时视音频回传通知失败: deviceId={}", userId);
            return;
        }

        publisher.publishEvent(new ServerNotifyEvent(this, notify.getDeviceId(), notify));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
