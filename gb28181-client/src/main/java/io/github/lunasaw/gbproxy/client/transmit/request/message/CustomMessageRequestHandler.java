package io.github.lunasaw.gbproxy.client.transmit.request.message;

import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * MESSAGE请求业务处理器默认实现
 * 提供默认的业务逻辑处理实现
 *
 * @author luna
 * @date 2023/10/18
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler")
public class CustomMessageRequestHandler implements MessageRequestHandler {

    @Override
    public DeviceRecord getDeviceRecord(DeviceRecordQuery deviceRecordQuery) {
        log.info("获取设备录像信息: {}", deviceRecordQuery);
        return new DeviceRecord();
    }

    @Override
    public DeviceStatus getDeviceStatus(String userId) {
        log.info("获取设备状态信息: {}", userId);
        return new DeviceStatus();
    }

    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        log.info("获取设备信息: {}", userId);
        return new DeviceInfo();
    }

    @Override
    public DeviceResponse getDeviceItem(String userId) {
        log.info("获取设备通道信息: {}", userId);
        return new DeviceResponse();
    }

    @Override
    public void broadcastNotify(DeviceBroadcastNotify broadcastNotify) {
        log.info("处理语音广播通知: {}", broadcastNotify);
    }

    @Override
    public DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery deviceAlarmQuery) {
        log.info("获取设备告警通知: {}", deviceAlarmQuery);
        return new DeviceAlarmNotify();
    }

    @Override
    public DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload deviceConfigDownload) {
        log.info("获取设备配置响应: {}", deviceConfigDownload);
        return new DeviceConfigResponse();
    }

    @Override
    public <T> void deviceControl(T deviceControlBase) {
        log.info("处理设备控制命令: {}", deviceControlBase);
    }

    @Override
    public PresetQueryResponse getDevicePresetQueryResponse(PresetQuery presetQuery) {
        return null;
    }

    @Override
    public MobilePositionNotify getMobilePositionNotify(MobilePositionQuery mobilePositionQuery) {
        return null;
    }

    @Override
    public PresetQueryResponse getPresetQueryResponse(String userId) {
        log.info("获取设备预置位信息: {}", userId);
        return new PresetQueryResponse();
    }

    @Override
    public ConfigDownloadResponse getConfigDownloadResponse(String userId, String configType) {
        log.info("获取设备配置查询应答: {}, configType={}", userId, configType);
        return new ConfigDownloadResponse();
    }
}