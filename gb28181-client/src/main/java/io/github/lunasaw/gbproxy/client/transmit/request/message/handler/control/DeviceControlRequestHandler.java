package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import io.github.lunasaw.gb28181.common.entity.control.*;

/**
 * DeviceControl控制命令业务处理器接口
 * 负责处理所有DeviceControl相关的控制命令
 *
 * @author luna
 */
public interface DeviceControlRequestHandler {

    /**
     * 处理云台控制命令
     */
    void handlePtzCmd(DeviceControlPtz ptzCmd);

    /**
     * 处理远程启动命令
     */
    void handleTeleBoot(DeviceControlTeleBoot teleBootCmd);

    /**
     * 处理录像控制命令
     */
    void handleRecordCmd(DeviceControlRecordCmd recordCmd);

    /**
     * 处理布防/撤防命令
     */
    void handleGuardCmd(DeviceControlGuard guardCmd);

    /**
     * 处理告警复位命令
     */
    void handleAlarmCmd(DeviceControlAlarm alarmCmd);

    /**
     * 处理强制关键帧命令
     */
    void handleIFameCmd(DeviceControlIFame iFameCmd);

    /**
     * 处理拉框放大命令
     */
    void handleDragZoomIn(DeviceControlDragIn dragInCmd);

    /**
     * 处理拉框缩小命令
     */
    void handleDragZoomOut(DeviceControlDragOut dragOutCmd);

    /**
     * 处理看守位命令
     */
    void handleHomePosition(DeviceControlPosition homePositionCmd);

    /**
     * 处理设备软件升级命令 (GB28181-2022 A.2.3.1.12)
     */
    default void handleDeviceUpgrade(DeviceUpgradeControl deviceUpgradeCmd) {
        // 默认空实现，便于现有实现类无侵入升级
    }

    /**
     * 处理 PTZ 精准控制命令 (GB28181-2022 A.2.3.1.11)
     */
    default void handlePtzPreciseCtrl(DeviceControlPTZPrecise ptzPreciseCtrl) {
        // 默认空实现
    }

    /**
     * 处理存储卡格式化命令 (GB28181-2022 A.2.3.1.13)
     */
    default void handleFormatSDCard(DeviceControlSDCardFormat formatSdCardCmd) {
        // 默认空实现
    }

    /**
     * 处理目标跟踪命令 (GB28181-2022 A.2.3.1.14)
     */
    default void handleTargetTrack(DeviceControlTargetTrack targetTrackCmd) {
        // 默认空实现
    }

    // 可扩展更多控制命令
}