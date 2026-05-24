package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.14 / A.2.3.2.12 收到图像抓拍配置命令事件
 *
 * @author luna
 */
@Getter
public class ClientSnapShotConfigEvent extends ApplicationEvent {

    private final String deviceId;
    private final SnapShotConfig snapShotConfig;

    public ClientSnapShotConfigEvent(Object source, String deviceId, SnapShotConfig snapShotConfig) {
        super(source);
        this.deviceId = deviceId;
        this.snapShotConfig = snapShotConfig;
    }
}
