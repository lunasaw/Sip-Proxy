package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientAlarmReportConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientAlarmSubscribeEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientCruiseTrackListQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientCruiseTrackQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientHomePositionQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientOsdConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientPtzPositionQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSdCardStatusQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSnapShotConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientVideoAlarmRecordConfigEvent;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * 集成测试用 client 端事件聚合监听器。
 */
@Component
@RequiredArgsConstructor
public class TestClientEventHandler {

    private final ClientDeviceSupplier clientDeviceSupplier;

    @Getter private volatile ClientSnapShotConfigEvent lastSnapShotConfig;
    @Getter private volatile ClientPtzPositionQueryEvent lastPtzPositionQuery;
    @Getter private volatile ClientSdCardStatusQueryEvent lastSdCardStatusQuery;
    @Getter private volatile ClientHomePositionQueryEvent lastHomePositionQuery;
    @Getter private volatile ClientCruiseTrackListQueryEvent lastCruiseTrackListQuery;
    @Getter private volatile ClientCruiseTrackQueryEvent lastCruiseTrackQuery;
    @Getter private volatile ClientAlarmSubscribeEvent lastAlarmSubscribe;
    @Getter private volatile ClientOsdConfigEvent lastOsdConfig;
    @Getter private volatile ClientVideoAlarmRecordConfigEvent lastVideoAlarmRecordConfig;
    @Getter private volatile ClientAlarmReportConfigEvent lastAlarmReportConfig;

    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        lastSnapShotConfig = null;
        lastPtzPositionQuery = null;
        lastSdCardStatusQuery = null;
        lastHomePositionQuery = null;
        lastCruiseTrackListQuery = null;
        lastCruiseTrackQuery = null;
        lastAlarmSubscribe = null;
        lastOsdConfig = null;
        lastVideoAlarmRecordConfig = null;
        lastAlarmReportConfig = null;
    }

    private void signal() { if (latch != null) latch.countDown(); }

    @EventListener public void onSnapShotConfig(ClientSnapShotConfigEvent e) { lastSnapShotConfig = e; signal(); }

    @EventListener
    public void onPtzPositionQuery(ClientPtzPositionQueryEvent e) {
        lastPtzPositionQuery = e;
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice to = (ToDevice) clientDeviceSupplier.getDevice(e.getSipId());
        if (from != null && to != null) {
            PTZPositionResponse response = new PTZPositionResponse(e.getQuery().getSn(), e.getUserId());
            response.setPan(180.0);
            response.setTilt(30.0);
            response.setZoom(2.0);
            ClientCommandSender.sendPtzPositionResponse(from, to, response);
        }
        signal();
    }

    @EventListener
    public void onSdCardStatusQuery(ClientSdCardStatusQueryEvent e) {
        lastSdCardStatusQuery = e;
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice to = (ToDevice) clientDeviceSupplier.getDevice(e.getSipId());
        if (from != null && to != null) {
            SDCardStatusResponse response = new SDCardStatusResponse(e.getQuery().getSn(), e.getUserId());
            response.setSumNum(1);
            SDCardStatusResponse.SDCardItem item = new SDCardStatusResponse.SDCardItem();
            item.setId(1);
            item.setHddName("SD1");
            item.setStatus("ok");
            item.setCapacity(131072);
            item.setFreeSpace(65536);
            response.setSdCardStatusInfo(new SDCardStatusResponse.SDCardStatusInfo(1, Collections.singletonList(item)));
            ClientCommandSender.sendSdCardStatusResponse(from, to, response);
        }
        signal();
    }

    @EventListener
    public void onHomePositionQuery(ClientHomePositionQueryEvent e) {
        lastHomePositionQuery = e;
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice to = (ToDevice) clientDeviceSupplier.getDevice(e.getSipId());
        if (from != null && to != null) {
            HomePositionResponse response = new HomePositionResponse(e.getQuery().getSn(), e.getUserId());
            response.setHomePosition(new HomePositionResponse.HomePositionInfo(1, 60, 1));
            ClientCommandSender.sendHomePositionResponse(from, to, response);
        }
        signal();
    }

    @EventListener
    public void onCruiseTrackListQuery(ClientCruiseTrackListQueryEvent e) {
        lastCruiseTrackListQuery = e;
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice to = (ToDevice) clientDeviceSupplier.getDevice(e.getSipId());
        if (from != null && to != null) {
            CruiseTrackListResponse response = new CruiseTrackListResponse(e.getQuery().getSn(), e.getUserId());
            response.setSumNum(2);
            CruiseTrackListResponse.CruiseTrack t1 = new CruiseTrackListResponse.CruiseTrack(0, "Track-A");
            CruiseTrackListResponse.CruiseTrack t2 = new CruiseTrackListResponse.CruiseTrack(1, "Track-B");
            response.setCruiseTrackList(new CruiseTrackListResponse.CruiseTrackList(2, java.util.Arrays.asList(t1, t2)));
            ClientCommandSender.sendCruiseTrackListResponse(from, to, response);
        }
        signal();
    }

    @EventListener
    public void onCruiseTrackQuery(ClientCruiseTrackQueryEvent e) {
        lastCruiseTrackQuery = e;
        FromDevice from = clientDeviceSupplier.getClientFromDevice();
        ToDevice to = (ToDevice) clientDeviceSupplier.getDevice(e.getSipId());
        if (from != null && to != null) {
            CruiseTrackResponse response = new CruiseTrackResponse(e.getQuery().getSn(), e.getUserId());
            response.setNumber(e.getQuery().getNumber());
            response.setName("Track-A");
            response.setSumNum(2);
            CruiseTrackResponse.CruisePoint p1 = new CruiseTrackResponse.CruisePoint(1, 5, 8);
            CruiseTrackResponse.CruisePoint p2 = new CruiseTrackResponse.CruisePoint(2, 3, 8);
            response.setCruisePointList(new CruiseTrackResponse.CruisePointList(2, java.util.Arrays.asList(p1, p2)));
            ClientCommandSender.sendCruiseTrackResponse(from, to, response);
        }
        signal();
    }

    @EventListener public void onAlarmSubscribe(ClientAlarmSubscribeEvent e) { lastAlarmSubscribe = e; signal(); }
    @EventListener public void onOsdConfig(ClientOsdConfigEvent e) { lastOsdConfig = e; signal(); }
    @EventListener public void onVideoAlarmRecord(ClientVideoAlarmRecordConfigEvent e) { lastVideoAlarmRecordConfig = e; signal(); }
    @EventListener public void onAlarmReport(ClientAlarmReportConfigEvent e) { lastAlarmReportConfig = e; signal(); }
}
