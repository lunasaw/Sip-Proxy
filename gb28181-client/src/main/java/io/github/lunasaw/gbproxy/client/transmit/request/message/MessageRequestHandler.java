package io.github.lunasaw.gbproxy.client.transmit.request.message;

import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;

/**
 * MESSAGE请求业务处理器接口
 * 负责处理MESSAGE请求的业务逻辑，包括查询、控制、通知等
 *
 * @author luna
 * @date 2023/10/18
 */
public interface MessageRequestHandler {

    /**
     * 获取设备录像信息
     * DeviceRecord
     *
     * @param deviceRecordQuery 设备录像查询
     * @return DeviceRecord 设备录像信息
     */
    DeviceRecord getDeviceRecord(DeviceRecordQuery deviceRecordQuery);

    /**
     * 获取设备状态信息
     * DeviceStatus
     *
     * @param userId 设备Id
     * @return DeviceStatus 设备状态信息
     */
    DeviceStatus getDeviceStatus(String userId);

    /**
     * 获取设备信息
     * DeviceInfo
     *
     * @param userId 设备Id
     * @return DeviceInfo 设备信息
     */
    DeviceInfo getDeviceInfo(String userId);

    /**
     * 获取设备通道信息
     *
     * @param userId 设备Id
     * @return DeviceResponse 设备通道信息
     */
    DeviceResponse getDeviceItem(String userId);

    /**
     * 处理语音广播通知
     *
     * @param broadcastNotify 广播通知
     */
    void broadcastNotify(DeviceBroadcastNotify broadcastNotify);

    /**
     * 获取设备告警通知
     *
     * @param deviceAlarmQuery 告警查询
     * @return DeviceAlarmNotify 告警通知
     */
    DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery deviceAlarmQuery);

    /**
     * 获取设备配置响应
     *
     * @param deviceConfigDownload 配置下载查询
     * @return DeviceConfigResponse 配置响应
     */
    DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload deviceConfigDownload);

    /**
     * 处理设备控制命令
     *
     * @param deviceControlBase 设备控制基础信息
     */
    <T> void deviceControl(T deviceControlBase);

    /**
     * 获取设备预置位查询应答
     *
     * @return 预置位查询应答
     */
    PresetQueryResponse getDevicePresetQueryResponse(PresetQuery presetQuery);

    /**
     * 获取设备预置位信息
     *
     * @param userId 设备Id
     * @return 设备预置位应答
     */
    PresetQueryResponse getPresetQueryResponse(String userId);

    /**
     * 获取设备配置查询应答
     *
     * @param userId     设备Id
     * @param configType 配置类型
     * @return 设备配置查询应答
     */
    ConfigDownloadResponse getConfigDownloadResponse(String userId, String configType);

    /**
     * 处理设备移动位置通知
     *
     * @param mobilePositionNotify 移动位置通知
     */
    MobilePositionNotify getMobilePositionNotify(MobilePositionQuery mobilePositionQuery);

}
