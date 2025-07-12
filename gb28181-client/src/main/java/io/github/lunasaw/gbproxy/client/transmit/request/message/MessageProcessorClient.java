package io.github.lunasaw.gbproxy.client.transmit.request.message;

import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;

/**
 * MESSAGE请求业务处理器接口
 * 负责处理MESSAGE请求的业务逻辑，包括查询、控制、通知等
 *
 * @author luna
 * @date 2023/10/18
 */
public interface MessageProcessorClient {

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
}
