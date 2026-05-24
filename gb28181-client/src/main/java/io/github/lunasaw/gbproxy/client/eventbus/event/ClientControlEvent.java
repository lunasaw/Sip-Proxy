package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：平台控制（rootType=Control，cmdType=DeviceControl）。
 *
 * <p>承载 13 个 control 类的多态 payload，全部直接 extends {@code DeviceControlBase}：
 * DeviceControlPtz / DeviceControlTeleBoot / DeviceControlRecordCmd / DeviceControlGuard /
 * DeviceControlAlarm / DeviceControlIFame / DeviceControlDragIn / DeviceControlDragOut /
 * DeviceControlPosition / DeviceUpgradeControl / DeviceControlPTZPrecise /
 * DeviceControlSDCardFormat / DeviceControlTargetTrack。
 *
 * <p>{@code KeepaliveControl} 虽然也间接继承 {@code DeviceControlBase}（KeepaliveControl→ControlBase→
 * DeviceControlBase），但走独立的 {@link ClientKeepaliveEvent} —— L0 的
 * {@code KeepaliveMessageClientHandler}（cmdType=Keepalive）发布独立事件，所以本事件不会
 * 收到 KeepaliveControl 实例。
 */
@Getter
public class ClientControlEvent extends ApplicationEvent {

    private final String userId;
    private final DeviceControlBase command;

    public ClientControlEvent(Object source, String userId, DeviceControlBase command) {
        super(source);
        this.userId = userId;
        this.command = command;
    }
}
