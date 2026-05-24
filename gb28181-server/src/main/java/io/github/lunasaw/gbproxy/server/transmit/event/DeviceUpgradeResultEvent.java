package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
import lombok.Getter;

/**
 * GB28181-2022 §9.13 / A.2.5.9 设备软件升级结果通知事件
 *
 * @author luna
 */
@Getter
public class DeviceUpgradeResultEvent extends DeviceEvent {

    private final UpgradeResultNotify notify;

    public DeviceUpgradeResultEvent(Object source, String deviceId, UpgradeResultNotify notify) {
        super(source, deviceId);
        this.notify = notify;
    }
}
