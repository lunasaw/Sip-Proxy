package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UploadSnapShotFinishedNotify;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gbproxy.server.api.ServerGb28181Adapter;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 集成测试用 server 端一站式 listener 实现（v1.5.x：取代旧 TestServerEventHandler 的 @EventListener
 * 散点形式，承载 Catalog/Info/Status/Alarm/Keepalive/Record/InviteOk/InviteFailure/SubscribeResponse/
 * UpgradeResult/SnapShotFinished/PtzPosition/SDCardStatus/HomePosition/CruiseTrack 全部最近一次记录）。
 *
 * <p>FlowTest 通过 reset(latch)+typed getter 断言，accessor 直接返回 typed payload。
 */
@Component
public class TestServerEventHandler extends ServerGb28181Adapter {

    @Getter private volatile DeviceResponse lastCatalog;
    @Getter private volatile DeviceInfo lastInfo;
    @Getter private volatile DeviceStatus lastStatus;
    @Getter private volatile DeviceAlarmNotify lastAlarm;
    @Getter private volatile DeviceKeepLiveNotify lastKeepalive;
    @Getter private volatile DeviceRecord lastRecord;
    @Getter private volatile String lastInviteOkCallId;
    @Getter private volatile InviteFailureRecord lastInviteFailure;
    @Getter private volatile SubscribeResponseRecord lastSubscribeResponse;
    @Getter private volatile UpgradeResultNotify lastUpgradeResult;
    @Getter private volatile UploadSnapShotFinishedNotify lastSnapShotFinished;
    @Getter private volatile PTZPositionResponse lastPtzPosition;
    @Getter private volatile SDCardStatusResponse lastSdCardStatus;
    @Getter private volatile HomePositionResponse lastHomePosition;
    @Getter private volatile CruiseTrackListResponse lastCruiseTrackList;
    @Getter private volatile CruiseTrackResponse lastCruiseTrack;

    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        lastCatalog = null; lastInfo = null; lastStatus = null;
        lastAlarm = null; lastKeepalive = null; lastRecord = null;
        lastInviteOkCallId = null; lastInviteFailure = null; lastSubscribeResponse = null;
        lastUpgradeResult = null;
        lastSnapShotFinished = null;
        lastPtzPosition = null;
        lastSdCardStatus = null;
        lastHomePosition = null;
        lastCruiseTrackList = null;
        lastCruiseTrack = null;
    }

    private void signal() { if (latch != null) latch.countDown(); }

    public record InviteFailureRecord(String callId, int statusCode) {}
    public record SubscribeResponseRecord(String callId, int statusCode) {}

    @Override public void onCatalogResponse(String deviceId, String sn, DeviceResponse catalog) { lastCatalog = catalog; signal(); }
    @Override public void onDeviceInfoResponse(String deviceId, String sn, DeviceInfo info) { lastInfo = info; signal(); }
    @Override public void onDeviceStatusResponse(String deviceId, String sn, DeviceStatus status) { lastStatus = status; signal(); }
    @Override public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) { lastAlarm = notify; signal(); }
    @Override public void onKeepalive(String deviceId, DeviceKeepLiveNotify notify) { lastKeepalive = notify; signal(); }
    @Override public void onRecordInfoResponse(String deviceId, String sn, DeviceRecord record) { lastRecord = record; signal(); }
    @Override public void onInviteOk(String deviceId, String callId) { lastInviteOkCallId = callId; signal(); }
    @Override public void onInviteFailure(String deviceId, String callId, int statusCode) { lastInviteFailure = new InviteFailureRecord(callId, statusCode); signal(); }
    @Override public void onSubscribeResponse(String deviceId, String callId, int statusCode) { lastSubscribeResponse = new SubscribeResponseRecord(callId, statusCode); signal(); }
    @Override public void onUpgradeResult(String deviceId, UpgradeResultNotify notify) { lastUpgradeResult = notify; signal(); }
    @Override public void onSnapShotFinished(String deviceId, UploadSnapShotFinishedNotify notify) { lastSnapShotFinished = notify; signal(); }
    @Override public void onPtzPositionResponse(String deviceId, PTZPositionResponse response) { lastPtzPosition = response; signal(); }
    @Override public void onSdCardStatusResponse(String deviceId, SDCardStatusResponse response) { lastSdCardStatus = response; signal(); }
    @Override public void onHomePositionResponse(String deviceId, HomePositionResponse response) { lastHomePosition = response; signal(); }
    @Override public void onCruiseTrackListResponse(String deviceId, CruiseTrackListResponse response) { lastCruiseTrackList = response; signal(); }
    @Override public void onCruiseTrackResponse(String deviceId, CruiseTrackResponse response) { lastCruiseTrack = response; signal(); }
}
