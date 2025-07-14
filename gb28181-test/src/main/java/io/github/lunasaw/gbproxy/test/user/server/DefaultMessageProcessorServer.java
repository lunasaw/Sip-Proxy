package io.github.lunasaw.gbproxy.test.user.server;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.server.transimit.request.message.MessageProcessorServer;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.extern.slf4j.Slf4j;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;

/**
 * @author luna
 * @date 2023/12/25
 */
@Component
@Slf4j
public class DefaultMessageProcessorServer implements MessageProcessorServer {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Autowired
    private DeviceSupplier        deviceSupplier;
    @Override
    public void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify) {
        log.info("接收到设备的心跳 keepLiveDevice::deviceKeepLiveNotify = {}", JSON.toJSONString(deviceKeepLiveNotify));
    }

    @Override
    public void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo) {
        log.info("接收到设备的地址信息 updateRemoteAddress::remoteAddressInfo = {}", remoteAddressInfo);
        Device device = sipUserGenerateServer.getFromDevice();
        if (device == null) {
            return;
        }
        device.setIp(remoteAddressInfo.getIp());
        device.setPort(remoteAddressInfo.getPort());
        deviceSupplier.addOrUpdateDevice(device);
    }

    @Override
    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {
        log.info("接收到设备的告警信息 updateDeviceAlarm::deviceAlarmNotify = {}", deviceAlarmNotify);
    }

    @Override
    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {
        log.info("接收到设备的位置信息 updateMobilePosition::mobilePositionNotify = {}", mobilePositionNotify);
    }

    @Override
    public void updateMediaStatus(MediaStatusNotify mediaStatusNotify) {
        log.info("接收到设备的媒体状态信息 updateMediaStatus::mediaStatusNotify = {}", mediaStatusNotify);
    }

    @Override
    public void updateDeviceRecord(String userId, DeviceRecord deviceRecord) {
        log.info("接收到设备的录像信息 updateDeviceRecord::userId = {}, deviceRecord = {}", userId, JSON.toJSONString(deviceRecord));
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {
        log.info("接收到设备通道信息 updateDeviceResponse::userId = {}, deviceResponse = {}", userId, JSON.toJSONString(deviceResponse));
    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {
        log.info("接收到设备信息 updateDeviceInfo::userId = {}, deviceInfo = {}", userId, JSON.toJSONString(deviceInfo));
    }
}
