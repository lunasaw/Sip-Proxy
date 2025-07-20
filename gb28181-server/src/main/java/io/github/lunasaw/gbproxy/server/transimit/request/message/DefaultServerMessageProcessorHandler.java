package io.github.lunasaw.gbproxy.server.transimit.request.message;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * Server模块MESSAGE请求处理器业务接口默认实现
 *
 * @author luna
 */
@Slf4j
public class DefaultServerMessageProcessorHandler implements ServerMessageProcessorHandler {

    @Override
    public void handleMessageRequest(RequestEvent evt, FromDevice fromDevice) {
        log.debug("默认处理MESSAGE请求：事件 = {}, 发送设备 = {}", evt, fromDevice);
    }

    @Override
    public boolean validateDevicePermission(RequestEvent evt) {
        log.debug("默认验证设备权限：事件 = {}", evt);
        return true;
    }

    @Override
    public FromDevice getFromDevice() {
        log.debug("默认获取发送设备信息");
        return null;
    }

    @Override
    public void handleMessageError(RequestEvent evt, String errorMessage) {
        log.debug("默认处理MESSAGE请求错误：事件 = {}, 错误消息 = {}", evt, errorMessage);
    }

    @Override
    public void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify) {

    }

    @Override
    public void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo) {

    }

    @Override
    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {

    }

    @Override
    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {

    }

    @Override
    public void updateMediaStatus(MediaStatusNotify mediaStatusNotify) {

    }

    @Override
    public void updateDeviceRecord(String userId, DeviceRecord deviceRecord) {

    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {

    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {

    }

    @Override
    public void updateDeviceConfig(String userId, DeviceConfigResponse deviceConfigResponse) {

    }

    @Override
    public void updateDeviceStatus(String userId, DeviceStatus deviceStatus) {

    }
}