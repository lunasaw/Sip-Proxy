package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台配置（rootType=Control，cmdType=DeviceConfig）。
 *
 * <p>承载 5 个 config 类的多态 payload —— SnapShotConfig / OsdConfig / AlarmReportConfig /
 * VideoAlarmRecordConfig / DeviceConfigControl 全部**直接** extends {@code DeviceControlBase}，
 * 互为兄弟节点（不是父子关系）。Adapter 用 {@code Class<?> → Consumer} 映射表分发，
 * 当前 instanceof 顺序不敏感，但映射表能在未来父子化重构时免改并支持单元测试遍历断言完整性。
 */
@Getter
public class ClientConfigEvent extends ApplicationEvent {

    private final String userId;
    private final DeviceControlBase config;

    public ClientConfigEvent(Object source, String userId, DeviceControlBase config) {
        super(source);
        this.userId = userId;
        this.config = config;
    }
}
