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

    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        lastCatalog = null; lastInfo = null; lastStatus = null;
        lastAlarm = null; lastKeepalive = null; lastRecord = null;
        lastInviteOk = null; lastInviteFailure = null; lastSubscribeResponse = null;
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
}
