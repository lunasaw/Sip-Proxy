package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.5 / A.2.3.2.11 收到 OSD 配置命令事件
 *
 * @author luna
 */
@Getter
public class ClientOsdConfigEvent extends ApplicationEvent {

    private final String deviceId;
    private final OsdConfig osdConfig;

    public ClientOsdConfigEvent(Object source, String deviceId, OsdConfig osdConfig) {
        super(source);
        this.deviceId = deviceId;
        this.osdConfig = osdConfig;
    }
}
