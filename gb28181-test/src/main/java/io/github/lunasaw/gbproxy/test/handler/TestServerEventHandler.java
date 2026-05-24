package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.event.*;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class TestServerEventHandler {

    @Getter private volatile DeviceCatalogEvent lastCatalog;
    @Getter private volatile DeviceInfoEvent lastInfo;
    @Getter private volatile DeviceStatusEvent lastStatus;
    @Getter private volatile DeviceAlarmEvent lastAlarm;
    @Getter private volatile DeviceKeepaliveEvent lastKeepalive;
    @Getter private volatile DeviceRecordEvent lastRecord;
    @Getter private volatile DeviceInviteOkEvent lastInviteOk;
    @Getter private volatile DeviceInviteFailureEvent lastInviteFailure;
    @Getter private volatile DeviceSubscribeResponseEvent lastSubscribeResponse;
    @Getter private volatile DeviceUpgradeResultEvent lastUpgradeResult;
    @Getter private volatile DeviceSnapShotFinishedEvent lastSnapShotFinished;
    @Getter private volatile DevicePtzPositionEvent lastPtzPosition;
    @Getter private volatile DeviceSdCardStatusEvent lastSdCardStatus;
    @Getter private volatile DeviceHomePositionEvent lastHomePosition;
    @Getter private volatile DeviceCruiseTrackEvent lastCruiseTrack;

    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        lastCatalog = null; lastInfo = null; lastStatus = null;
        lastAlarm = null; lastKeepalive = null; lastRecord = null;
        lastInviteOk = null; lastInviteFailure = null; lastSubscribeResponse = null;
        lastUpgradeResult = null;
        lastSnapShotFinished = null;
        lastPtzPosition = null;
        lastSdCardStatus = null;
        lastHomePosition = null;
        lastCruiseTrack = null;
    }

    private void signal() { if (latch != null) latch.countDown(); }

    @EventListener public void onCatalog(DeviceCatalogEvent e) { lastCatalog = e; signal(); }
    @EventListener public void onInfo(DeviceInfoEvent e) { lastInfo = e; signal(); }
    @EventListener public void onStatus(DeviceStatusEvent e) { lastStatus = e; signal(); }
    @EventListener public void onAlarm(DeviceAlarmEvent e) { lastAlarm = e; signal(); }
    @EventListener public void onKeepalive(DeviceKeepaliveEvent e) { lastKeepalive = e; signal(); }
    @EventListener public void onRecord(DeviceRecordEvent e) { lastRecord = e; signal(); }
    @EventListener public void onInviteOk(DeviceInviteOkEvent e) { lastInviteOk = e; signal(); }
    @EventListener public void onInviteFailure(DeviceInviteFailureEvent e) { lastInviteFailure = e; signal(); }
    @EventListener public void onSubscribeResponse(DeviceSubscribeResponseEvent e) { lastSubscribeResponse = e; signal(); }
    @EventListener public void onUpgradeResult(DeviceUpgradeResultEvent e) { lastUpgradeResult = e; signal(); }
    @EventListener public void onSnapShotFinished(DeviceSnapShotFinishedEvent e) { lastSnapShotFinished = e; signal(); }
    @EventListener public void onPtzPosition(DevicePtzPositionEvent e) { lastPtzPosition = e; signal(); }
    @EventListener public void onSdCardStatus(DeviceSdCardStatusEvent e) { lastSdCardStatus = e; signal(); }
    @EventListener public void onHomePosition(DeviceHomePositionEvent e) { lastHomePosition = e; signal(); }
    @EventListener public void onCruiseTrack(DeviceCruiseTrackEvent e) { lastCruiseTrack = e; signal(); }
}
