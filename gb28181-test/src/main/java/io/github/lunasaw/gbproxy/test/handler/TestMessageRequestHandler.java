package io.github.lunasaw.gbproxy.test.handler;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class TestMessageRequestHandler implements MessageRequestHandler {

    @Override
    public DeviceRecord getDeviceRecord(DeviceRecordQuery query) {
        DeviceRecord record = new DeviceRecord(CmdTypeEnum.RECORD_INFO.getType(), query.getSn(), query.getDeviceId());
        DeviceRecord.RecordItem item = new DeviceRecord.RecordItem();
        item.setDeviceId(query.getDeviceId());
        item.setName("TestRecord");
        item.setStartTime("2024-01-01T00:00:00");
        item.setEndTime("2024-01-01T01:00:00");
        record.setRecordList(List.of(item));
        return record;
    }

    @Override
    public DeviceStatus getDeviceStatus(String userId) {
        DeviceStatus status = new DeviceStatus(CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), userId);
        status.setOnline("ONLINE");
        status.setStatus("OK");
        return status;
    }

    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), RandomStrUtil.getValidationCode(), userId);
        info.setDeviceName("TestDevice");
        info.setManufacturer("TestManufacturer");
        info.setResult("OK");
        return info;
    }

    @Override
    public DeviceResponse getDeviceItem(String userId) {
        DeviceItem item = new DeviceItem();
        item.setDeviceId(userId);
        item.setName("TestChannel");
        DeviceResponse response = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), userId);
        response.setDeviceItemList(List.of(item));
        return response;
    }

    @Override
    public void broadcastNotify(DeviceBroadcastNotify broadcastNotify) {
    }

    @Override
    public DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery query) {
        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setDeviceId(query.getDeviceId());
        notify.setAlarmMethod("1");
        return notify;
    }

    @Override
    public DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload download) {
        return new DeviceConfigResponse();
    }

    @Override
    public <T> void deviceControl(T deviceControlBase) {
    }

    @Override
    public PresetQueryResponse getDevicePresetQueryResponse(PresetQuery presetQuery) {
        return new PresetQueryResponse();
    }

    @Override
    public PresetQueryResponse getPresetQueryResponse(String userId) {
        return new PresetQueryResponse();
    }

    @Override
    public ConfigDownloadResponse getConfigDownloadResponse(String userId, String configType) {
        return new ConfigDownloadResponse();
    }

    @Override
    public MobilePositionNotify getMobilePositionNotify(MobilePositionQuery query) {
        return new MobilePositionNotify();
    }
}
