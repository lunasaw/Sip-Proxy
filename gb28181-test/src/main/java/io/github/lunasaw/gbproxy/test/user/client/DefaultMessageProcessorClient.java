package io.github.lunasaw.gbproxy.test.user.client;

import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2023/10/17
 */
@Component
@Slf4j
public class DefaultMessageProcessorClient implements MessageProcessorClient {

    @Override
    public DeviceRecord getDeviceRecord(DeviceRecordQuery userId) {
        return (DeviceRecord)XmlUtils.parseFile("classpath:device/deviceRecord.xml", DeviceRecord.class);
    }

    @Override
    public DeviceStatus getDeviceStatus(String userId) {
        return (DeviceStatus)XmlUtils.parseFile("classpath:device/deviceStatus.xml", DeviceStatus.class);
    }

    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        return (DeviceInfo)XmlUtils.parseFile("classpath:device/deviceInfo.xml", DeviceInfo.class);
    }

    @Override
    public DeviceResponse getDeviceItem(String userId) {
        DeviceResponse response = (DeviceResponse)XmlUtils.parseFile("classpath:device/catalog.xml", DeviceResponse.class);
        return response;
    }

    @Override
    public void broadcastNotify(DeviceBroadcastNotify broadcastNotify) {
        log.info("broadcastNotify::broadcastNotify = {}", broadcastNotify);
    }

    @Override
    public DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery deviceAlarmQuery) {
        return null;
    }

    @Override
    public DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload deviceConfigDownload) {
        return null;
    }

    @Override
    public <T> void deviceControl(T deviceControlBase) {
        log.info("deviceControl::deviceControlBase = {}", deviceControlBase);
    }
}
