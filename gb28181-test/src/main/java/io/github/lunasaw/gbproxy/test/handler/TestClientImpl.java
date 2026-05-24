package io.github.lunasaw.gbproxy.test.handler;

import com.luna.common.text.RandomStrUtil;
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
import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackListQuery;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.query.HomePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PTZPositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.query.SDCardStatusQuery;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gbproxy.client.api.ClientGb28181Adapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 集成测试用 client 端一站式 listener 实现（v1.5.0 新增，替代旧 TestMessageRequestHandler /
 * TestDeviceControlHandler / TestClientEventHandler 三家分散的 mock）。
 *
 * <p>承担三类职责：
 * <ol>
 *   <li>实现 QueryListener 的 13 个查询方法，返回 mock response（Adapter 自动回包给 server）</li>
 *   <li>实现 ControlListener 的 14 个控制方法，记录 lastCommand 供测试断言</li>
 *   <li>实现 ConfigListener 的 5 个配置方法，记录 lastXxxConfig 供测试断言</li>
 *   <li>实现 SubscribeListener 的 3 个订阅方法，记录 lastXxxSubscribe</li>
 *   <li>实现 NotifyListener 的 Broadcast 通知方法</li>
 * </ol>
 *
 * <p>{@link #reset(CountDownLatch)} 会清空所有 last* 字段并装载新 latch；任何 listener 方法被调用时
 * 自动 {@code latch.countDown()}，便于 FlowTest 用 {@code latch.await(...)} 等待事件到达。
 *
 * @author luna
 */
@Primary
@Component
@Slf4j
public class TestClientImpl extends ClientGb28181Adapter {

    // ============ 控制命令最近一次（兼容旧 TestDeviceControlHandler.lastCommand）============
    @Getter
    private volatile Object lastCommand;

    // ============ 配置类最近一次 ============
    @Getter
    private volatile SnapShotConfig lastSnapShotConfig;
    @Getter
    private volatile OsdConfig lastOsdConfig;
    @Getter
    private volatile VideoAlarmRecordConfig lastVideoAlarmRecordConfig;
    @Getter
    private volatile AlarmReportConfig lastAlarmReportConfig;

    // ============ 查询���求最近一次（用于断言 server 发出的查询确实到达 client）============
    @Getter
    private volatile PTZPositionQuery lastPtzPositionQuery;
    @Getter
    private volatile SDCardStatusQuery lastSdCardStatusQuery;
    @Getter
    private volatile HomePositionQuery lastHomePositionQuery;
    @Getter
    private volatile CruiseTrackListQuery lastCruiseTrackListQuery;
    @Getter
    private volatile CruiseTrackQuery lastCruiseTrackQuery;

    // ============ 订阅最近一次 ============
    @Getter
    private volatile DeviceAlarmQuery lastAlarmSubscribe;
    @Getter
    private volatile DeviceQuery lastCatalogSubscribe;

    // ============ 通知最近一次 ============
    @Getter
    private volatile DeviceBroadcastNotify lastBroadcastNotify;

    // ============ latch 协调 ============
    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        this.lastCommand = null;
        this.lastSnapShotConfig = null;
        this.lastOsdConfig = null;
        this.lastVideoAlarmRecordConfig = null;
        this.lastAlarmReportConfig = null;
        this.lastPtzPositionQuery = null;
        this.lastSdCardStatusQuery = null;
        this.lastHomePositionQuery = null;
        this.lastCruiseTrackListQuery = null;
        this.lastCruiseTrackQuery = null;
        this.lastAlarmSubscribe = null;
        this.lastCatalogSubscribe = null;
        this.lastBroadcastNotify = null;
    }

    private void signal(Object captured) {
        // captured 仅用于调试，实际信号靠 countDown
        if (latch != null) {
            latch.countDown();
        }
    }

    // ===================== QueryListener =====================

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        DeviceItem item = new DeviceItem();
        item.setDeviceId(query.getDeviceId());
        item.setName("TestChannel");
        DeviceResponse response = new DeviceResponse(CmdTypeEnum.CATALOG.getType(),
                RandomStrUtil.getValidationCode(), query.getDeviceId());
        response.setDeviceItemList(List.of(item));
        return response;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(),
                RandomStrUtil.getValidationCode(), query.getDeviceId());
        info.setDeviceName("TestDevice");
        info.setManufacturer("TestManufacturer");
        info.setResult("OK");
        return info;
    }

    @Override
    public DeviceStatus onDeviceStatusQuery(String platformId, DeviceQuery query) {
        DeviceStatus status = new DeviceStatus(CmdTypeEnum.DEVICE_STATUS.getType(),
                RandomStrUtil.getValidationCode(), query.getDeviceId());
        status.setOnline("ONLINE");
        status.setStatus("OK");
        return status;
    }

    @Override
    public DeviceRecord onRecordInfoQuery(String platformId, DeviceRecordQuery query) {
        DeviceRecord record = new DeviceRecord(CmdTypeEnum.RECORD_INFO.getType(),
                query.getSn(), query.getDeviceId());
        DeviceRecord.RecordItem item = new DeviceRecord.RecordItem();
        item.setDeviceId(query.getDeviceId());
        item.setName("TestRecord");
        item.setStartTime("2024-01-01T00:00:00");
        item.setEndTime("2024-01-01T01:00:00");
        record.setRecordList(List.of(item));
        return record;
    }

    @Override
    public DeviceAlarmNotify onAlarmQuery(String platformId, DeviceAlarmQuery query) {
        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setDeviceId(query.getDeviceId());
        notify.setAlarmMethod("1");
        return notify;
    }

    @Override
    public PresetQueryResponse onPresetQuery(String platformId, PresetQuery query) {
        return new PresetQueryResponse();
    }

    @Override
    public MobilePositionNotify onMobilePositionQuery(String platformId, MobilePositionQuery query) {
        return new MobilePositionNotify();
    }

    @Override
    public PTZPositionResponse onPtzPositionQuery(String platformId, PTZPositionQuery query) {
        this.lastPtzPositionQuery = query;
        PTZPositionResponse response = new PTZPositionResponse(query.getSn(), query.getDeviceId());
        response.setPan(180.0);
        response.setTilt(30.0);
        response.setZoom(2.0);
        signal(response);
        return response;
    }

    @Override
    public SDCardStatusResponse onSdCardStatusQuery(String platformId, SDCardStatusQuery query) {
        this.lastSdCardStatusQuery = query;
        SDCardStatusResponse response = new SDCardStatusResponse(query.getSn(), query.getDeviceId());
        response.setSumNum(1);
        SDCardStatusResponse.SDCardItem item = new SDCardStatusResponse.SDCardItem();
        item.setId(1);
        item.setHddName("SD1");
        item.setStatus("ok");
        item.setCapacity(131072);
        item.setFreeSpace(65536);
        response.setSdCardStatusInfo(new SDCardStatusResponse.SDCardStatusInfo(1, Collections.singletonList(item)));
        signal(response);
        return response;
    }

    @Override
    public HomePositionResponse onHomePositionQuery(String platformId, HomePositionQuery query) {
        this.lastHomePositionQuery = query;
        HomePositionResponse response = new HomePositionResponse(query.getSn(), query.getDeviceId());
        response.setHomePosition(new HomePositionResponse.HomePositionInfo(1, 60, 1));
        signal(response);
        return response;
    }

    @Override
    public CruiseTrackListResponse onCruiseTrackListQuery(String platformId, CruiseTrackListQuery query) {
        this.lastCruiseTrackListQuery = query;
        CruiseTrackListResponse response = new CruiseTrackListResponse(query.getSn(), query.getDeviceId());
        response.setSumNum(2);
        CruiseTrackListResponse.CruiseTrack t1 = new CruiseTrackListResponse.CruiseTrack(0, "Track-A");
        CruiseTrackListResponse.CruiseTrack t2 = new CruiseTrackListResponse.CruiseTrack(1, "Track-B");
        response.setCruiseTrackList(new CruiseTrackListResponse.CruiseTrackList(2, Arrays.asList(t1, t2)));
        signal(response);
        return response;
    }

    @Override
    public CruiseTrackResponse onCruiseTrackQuery(String platformId, CruiseTrackQuery query) {
        this.lastCruiseTrackQuery = query;
        CruiseTrackResponse response = new CruiseTrackResponse(query.getSn(), query.getDeviceId());
        response.setNumber(query.getNumber());
        response.setName("Track-A");
        response.setSumNum(2);
        CruiseTrackResponse.CruisePoint p1 = new CruiseTrackResponse.CruisePoint(1, 5, 8);
        CruiseTrackResponse.CruisePoint p2 = new CruiseTrackResponse.CruisePoint(2, 3, 8);
        response.setCruisePointList(new CruiseTrackResponse.CruisePointList(2, Arrays.asList(p1, p2)));
        signal(response);
        return response;
    }

    // ===================== ControlListener =====================

    private void captureCommand(Object cmd) {
        this.lastCommand = cmd;
        signal(cmd);
    }

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) { captureCommand(cmd); }

    @Override
    public void onTeleBoot(String platformId, DeviceControlTeleBoot cmd) { captureCommand(cmd); }

    @Override
    public void onRecord(String platformId, DeviceControlRecordCmd cmd) { captureCommand(cmd); }

    @Override
    public void onGuard(String platformId, DeviceControlGuard cmd) { captureCommand(cmd); }

    @Override
    public void onAlarmReset(String platformId, DeviceControlAlarm cmd) { captureCommand(cmd); }

    @Override
    public void onIFrame(String platformId, DeviceControlIFame cmd) { captureCommand(cmd); }

    @Override
    public void onDragIn(String platformId, DeviceControlDragIn cmd) { captureCommand(cmd); }

    @Override
    public void onDragOut(String platformId, DeviceControlDragOut cmd) { captureCommand(cmd); }

    @Override
    public void onHomePositionControl(String platformId, DeviceControlPosition cmd) { captureCommand(cmd); }

    @Override
    public void onDeviceUpgrade(String platformId, DeviceUpgradeControl cmd) { captureCommand(cmd); }

    @Override
    public void onPtzPrecise(String platformId, DeviceControlPTZPrecise cmd) { captureCommand(cmd); }

    @Override
    public void onFormatSdCard(String platformId, DeviceControlSDCardFormat cmd) { captureCommand(cmd); }

    @Override
    public void onTargetTrack(String platformId, DeviceControlTargetTrack cmd) { captureCommand(cmd); }

    @Override
    public void onUnknownControl(String platformId, DeviceControlBase cmd) {
        log.debug("TestClientImpl 收到未识别的控制命令: {}", cmd.getClass().getSimpleName());
    }

    // ===================== ConfigListener =====================

    @Override
    public void onSnapShotConfig(String platformId, SnapShotConfig cfg) {
        this.lastSnapShotConfig = cfg;
        signal(cfg);
    }

    @Override
    public void onOsdConfig(String platformId, OsdConfig cfg) {
        this.lastOsdConfig = cfg;
        signal(cfg);
    }

    @Override
    public void onAlarmReportConfig(String platformId, AlarmReportConfig cfg) {
        this.lastAlarmReportConfig = cfg;
        signal(cfg);
    }

    @Override
    public void onVideoAlarmRecordConfig(String platformId, VideoAlarmRecordConfig cfg) {
        this.lastVideoAlarmRecordConfig = cfg;
        signal(cfg);
    }

    // ===================== SubscribeListener =====================

    @Override
    public void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {
        this.lastCatalogSubscribe = query;
        signal(query);
    }

    @Override
    public void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {
        this.lastAlarmSubscribe = query;
        signal(query);
    }

    // ===================== NotifyListener =====================

    @Override
    public void onBroadcastNotify(String platformId, DeviceBroadcastNotify notify) {
        this.lastBroadcastNotify = notify;
        signal(notify);
    }
}
