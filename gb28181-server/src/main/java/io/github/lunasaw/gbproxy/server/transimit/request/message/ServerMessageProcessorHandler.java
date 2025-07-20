package io.github.lunasaw.gbproxy.server.transimit.request.message;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;

import javax.sip.RequestEvent;

/**
 * Server模块MESSAGE请求处理器业务接口
 * 负责具体的MESSAGE请求业务逻辑实现
 *
 * @author luna
 */
public interface ServerMessageProcessorHandler {

    /**
     * 处理MESSAGE请求
     *
     * @param evt        请求事件
     * @param fromDevice 发送设备
     */
    default void handleMessageRequest(RequestEvent evt, FromDevice fromDevice) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 验证设备权限
     *
     * @param evt 请求事件
     * @return 是否有权限
     */
    default boolean validateDevicePermission(RequestEvent evt) {
        return true; // 默认验证通过
    }

    /**
     * 获取发送设备信息
     *
     * @return 发送设备
     */
    default FromDevice getFromDevice() {
        return null;
    }

    /**
     * 处理MESSAGE请求错误
     *
     * @param evt          请求事件
     * @param errorMessage 错误消息
     */
    default void handleMessageError(RequestEvent evt, String errorMessage) {
        // 默认实现为空，由业务方根据需要实现
    }



    /**
     * 更新设备心跳信息
     * @param deviceKeepLiveNotify
     */
    void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify);

    /**
     * 更新设备地址信息
     *
     * @param userId
     * @param remoteAddressInfo
     */
    void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo);

    /**
     * 更新报警信息
     * @param deviceAlarmNotify
     */
    void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify);

    /**
     * 更新位置信息
     * @param mobilePositionNotify
     */
    void updateMobilePosition(MobilePositionNotify mobilePositionNotify);

    /**
     * 更新媒体状态
     * @param mediaStatusNotify
     */
    void updateMediaStatus(MediaStatusNotify mediaStatusNotify);

    /**
     * 更新设备录像
     *
     * @param userId
     * @param deviceRecord
     */
    void updateDeviceRecord(String userId, DeviceRecord deviceRecord);

    /**
     * 更新设备通道
     *
     * @param userId
     * @param deviceResponse
     */
    void updateDeviceResponse(String userId, DeviceResponse deviceResponse);

    /**
     * 更新设备信息
     * @param userId
     * @param deviceInfo
     */
    void updateDeviceInfo(String userId, DeviceInfo deviceInfo);

    void updateDeviceConfig(String userId, DeviceConfigResponse deviceConfigResponse);

    void updateDeviceStatus(String userId, DeviceStatus deviceStatus);

}