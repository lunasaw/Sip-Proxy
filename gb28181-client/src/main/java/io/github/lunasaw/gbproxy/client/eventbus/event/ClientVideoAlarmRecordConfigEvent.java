package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.5 / A.2.3.2.7 收到报警录像配置命令事件
 *
 * @author luna
 */
@Getter
public class ClientVideoAlarmRecordConfigEvent extends ApplicationEvent {

    private final String deviceId;
    private final VideoAlarmRecordConfig config;

    public ClientVideoAlarmRecordConfigEvent(Object source, String deviceId, VideoAlarmRecordConfig config) {
        super(source);
        this.deviceId = deviceId;
        this.config = config;
    }
}
