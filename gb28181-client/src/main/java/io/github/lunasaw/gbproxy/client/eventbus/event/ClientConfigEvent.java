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

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 配置命令体，具体类型由 cmdType 决定。 */
    private final DeviceControlBase config;

    /**
     * 构造平台配置事件。
     *
     * @param source 事件来源对象
     * @param userId 本端设备编码
     * @param config 配置命令体
     */
    public ClientConfigEvent(Object source, String userId, DeviceControlBase config) {
        super(source);
        this.userId = userId;
        this.config = config;
    }
}
