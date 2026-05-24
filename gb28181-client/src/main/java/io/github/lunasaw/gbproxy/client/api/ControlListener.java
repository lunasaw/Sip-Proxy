package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlAlarm;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragIn;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragOut;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlGuard;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlIFame;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPTZPrecise;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPosition;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlRecordCmd;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlSDCardFormat;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTargetTrack;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.gb28181.common.entity.control.DeviceUpgradeControl;
import io.github.lunasaw.gb28181.common.entity.control.KeepaliveControl;

/**
 * 平台控制监听器（client 角色业务方实现）。
 *
 * <p>fire-and-forget：所有方法返回 void，控制命令的 200 OK 由协议层自动回。
 *
 * <p>多 listener 策略：全部调用（观察者模式 —— 业务、metrics、audit 可同时监听同一控制命令）。
 *
 * <p>方法集合涵盖：
 * <ul>
 *   <li>13 个 cmdType=DeviceControl 的 XML 子标签 → 对应 control 类</li>
 *   <li>cmdType=Keepalive 的 KeepaliveControl 心跳</li>
 * </ul>
 * 后两类入口由 L0 不同 handler 触发不同 L1 事件��最终汇聚到此 listener。
 *
 * @author luna
 */
public interface ControlListener {

    /** PTZ 控制（PTZCmd）。 */
    default void onPtzControl(String platformId, DeviceControlPtz cmd) {}

    /** 远程重启（TeleBoot）。 */
    default void onTeleBoot(String platformId, DeviceControlTeleBoot cmd) {}

    /** 录像控制（RecordCmd）。 */
    default void onRecord(String platformId, DeviceControlRecordCmd cmd) {}

    /** 设防/撤防（GuardCmd）。 */
    default void onGuard(String platformId, DeviceControlGuard cmd) {}

    /** 报警复位（AlarmCmd）。 */
    default void onAlarmReset(String platformId, DeviceControlAlarm cmd) {}

    /** 强制 I 帧（IFameCmd）。 */
    default void onIFrame(String platformId, DeviceControlIFame cmd) {}

    /** 拉框放大（DragZoomIn）。 */
    default void onDragIn(String platformId, DeviceControlDragIn cmd) {}

    /** 拉框缩小（DragZoomOut）。 */
    default void onDragOut(String platformId, DeviceControlDragOut cmd) {}

    /** 看守位控制（HomePosition）。 */
    default void onHomePositionControl(String platformId, DeviceControlPosition cmd) {}

    /** 设备升级（DeviceUpgrade，GB28181-2022 §A.2.3.6）。 */
    default void onDeviceUpgrade(String platformId, DeviceUpgradeControl cmd) {}

    /** PTZ 精确控制（PTZPreciseCtrl，GB28181-2022 §A.2.3.7）。 */
    default void onPtzPrecise(String platformId, DeviceControlPTZPrecise cmd) {}

    /** SD 卡格式化（FormatSDCard，GB28181-2022 §A.2.3.8）。 */
    default void onFormatSdCard(String platformId, DeviceControlSDCardFormat cmd) {}

    /** 目标跟踪（TargetTrack，GB28181-2022 §A.2.3.9）。 */
    default void onTargetTrack(String platformId, DeviceControlTargetTrack cmd) {}

    /** 平台心跳（cmdType=Keepalive）。 */
    default void onKeepalive(String platformId, KeepaliveControl keepalive) {}

    /**
     * 框架内部分发兜底：当 ClientControlEvent 携带的 control 类不属于上述 13 类时调用。
     * 业务方一般无需 override；保留此 hook 用于诊断和未来 control 子类的扩展。
     */
    default void onUnknownControl(String platformId, DeviceControlBase cmd) {}
}
