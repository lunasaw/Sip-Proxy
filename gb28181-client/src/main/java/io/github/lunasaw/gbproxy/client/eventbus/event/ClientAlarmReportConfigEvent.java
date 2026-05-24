package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.5 / A.2.3.2.10 收到报警上报开关配置命令事件
 *
 * @author luna
 */
@Getter
public class ClientAlarmReportConfigEvent extends ApplicationEvent {

    private final String deviceId;
    private final AlarmReportConfig config;

    public ClientAlarmReportConfigEvent(Object source, String deviceId, AlarmReportConfig config) {
        super(source);
        this.deviceId = deviceId;
        this.config = config;
    }
}
