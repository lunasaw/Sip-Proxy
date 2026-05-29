package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Server 端 Layer 1 协议事件：设备主动通知（Alarm / Keepalive / MediaStatus / MobilePosition / UpgradeResult / SnapShotFinished）。
 *
 * <p>承载多态 typed payload，由 {@code ServerListenerAdapter} 按 payload 类型分发到
 * {@code DeviceNotifyListener} 的对应方法。
 *
 * @author luna
 */
@Getter
public class ServerNotifyEvent extends ApplicationEvent {

    private final String deviceId;
    /** typed payload：DeviceAlarmNotify/DeviceKeepLiveNotify/MediaStatusNotify/MobilePositionNotify/UpgradeResultNotify/UploadSnapShotFinishedNotify */
    private final Object payload;

    public ServerNotifyEvent(Object source, String deviceId, Object payload) {
        super(source);
        this.deviceId = deviceId;
        this.payload = payload;
    }
}
