package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.UploadSnapShotFinishedNotify;
import lombok.Getter;

/**
 * GB28181-2022 §9.14 / A.2.5.7 图像抓拍传输完成通知事件
 *
 * @author luna
 */
@Getter
public class DeviceSnapShotFinishedEvent extends DeviceEvent {

    private final UploadSnapShotFinishedNotify notify;

    public DeviceSnapShotFinishedEvent(Object source, String deviceId, UploadSnapShotFinishedNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
