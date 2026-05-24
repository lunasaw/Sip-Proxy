package io.github.lunasaw.gbproxy.server.transmit.event.internal;

import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceResponseListener;
import io.github.lunasaw.gbproxy.server.api.DeviceSessionListener;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAckEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAlarmEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceByeErrorEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceByeEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceCatalogEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceConfigEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceCruiseTrackEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceHomePositionEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoErrorEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInfoRequestEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInviteFailureEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInviteOkEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceInviteTryingEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceKeepaliveEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceMediaStatusEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceMobilePositionEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceNotifyUpdateEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOfflineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOnlineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DevicePtzPositionEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRecordEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterChallengeEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRemoteAddressEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceSdCardStatusEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceSnapShotFinishedEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceStatusEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceSubscribeResponseEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceUpgradeResultEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Server 端 Listener 分发器：监听 32 个 {@code Device*Event} / {@code ServerInviteEvent}，
 * 转发到 4 个 listener 接口。
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有 listener 用 {@code List<>} 注入，全部调用（观察者模式）</li>
 *   <li>无状态单例，业务方 0 个 listener 时所有事件正常通过到老的 @EventListener，新分发不影响行为</li>
 *   <li>底层的 32 个 Device*Event 仍然存在，业务侧若已经在监听这些 typed event 也可继续工作</li>
 * </ul>
 *
 * @author luna
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServerListenerAdapter {

    private final ObjectProvider<List<DeviceResponseListener>> responseListeners;
    private final ObjectProvider<List<DeviceNotifyListener>> notifyListeners;
    private final ObjectProvider<List<DeviceLifecycleListener>> lifecycleListeners;
    private final ObjectProvider<List<DeviceSessionListener>> sessionListeners;

    private static <T> List<T> safe(ObjectProvider<List<T>> provider) {
        List<T> list = provider.getIfAvailable();
        return list != null ? list : List.of();
    }

    // ============ Response listeners ============

    @EventListener
    public void on(DeviceCatalogEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onCatalogResponse(e.getDeviceId(), e.getSn(), e.getCatalog());
        }
    }

    @EventListener
    public void on(DeviceInfoEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onDeviceInfoResponse(e.getDeviceId(), e.getSn(), e.getInfo());
        }
    }

    @EventListener
    public void on(DeviceInfoErrorEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onDeviceInfoError(e.getDeviceId(), e.getErrorMessage());
        }
    }

    @EventListener
    public void on(DeviceInfoRequestEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onDeviceInfoRequest(e.getDeviceId(), e.getContent());
        }
    }

    @EventListener
    public void on(DeviceStatusEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onDeviceStatusResponse(e.getDeviceId(), e.getSn(), e.getStatus());
        }
    }

    @EventListener
    public void on(DeviceRecordEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onRecordInfoResponse(e.getDeviceId(), e.getSn(), e.getRecord());
        }
    }

    @EventListener
    public void on(DevicePtzPositionEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onPtzPositionResponse(e.getDeviceId(), e.getResponse());
        }
    }

    @EventListener
    public void on(DeviceSdCardStatusEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onSdCardStatusResponse(e.getDeviceId(), e.getResponse());
        }
    }

    @EventListener
    public void on(DeviceHomePositionEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onHomePositionResponse(e.getDeviceId(), e.getResponse());
        }
    }

    @EventListener
    public void on(DeviceCruiseTrackEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            if (e.getType() == DeviceCruiseTrackEvent.Type.LIST) {
                l.onCruiseTrackListResponse(e.getDeviceId(), e.getListResponse());
            } else {
                l.onCruiseTrackResponse(e.getDeviceId(), e.getTrackResponse());
            }
        }
    }

    @EventListener
    public void on(DeviceConfigEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onConfigResponse(e.getDeviceId(), e.getSn(), e.getConfig());
        }
    }

    @EventListener
    public void on(DeviceSubscribeResponseEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onSubscribeResponse(e.getDeviceId(), e.getCallId(), e.getStatusCode());
        }
    }

    @EventListener
    public void on(DeviceNotifyUpdateEvent e) {
        for (DeviceResponseListener l : safe(responseListeners)) {
            l.onNotifyUpdate(e.getDeviceId(), e.getNotify());
        }
    }

    // ============ Notify listeners ============

    @EventListener
    public void on(DeviceAlarmEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onAlarmNotify(e.getDeviceId(), e.getNotify());
        }
    }

    @EventListener
    public void on(DeviceKeepaliveEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onKeepalive(e.getDeviceId(), e.getNotify());
        }
    }

    @EventListener
    public void on(DeviceMediaStatusEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onMediaStatus(e.getDeviceId(), e.getNotify());
        }
    }

    @EventListener
    public void on(DeviceMobilePositionEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onMobilePositionNotify(e.getDeviceId(), e.getNotify());
        }
    }

    @EventListener
    public void on(DeviceUpgradeResultEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onUpgradeResult(e.getDeviceId(), e.getNotify());
        }
    }

    @EventListener
    public void on(DeviceSnapShotFinishedEvent e) {
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            l.onSnapShotFinished(e.getDeviceId(), e.getNotify());
        }
    }

    // ============ Lifecycle listeners ============

    @EventListener
    public void on(DeviceRegisterEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            l.onDeviceRegister(e.getDeviceId(), e.getRegisterInfo());
        }
    }

    @EventListener
    public void on(DeviceRegisterChallengeEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            l.onRegisterChallenge(e.getDeviceId());
        }
    }

    @EventListener
    public void on(DeviceOnlineEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            l.onDeviceOnline(e.getDeviceId(), e.getSipTransaction());
        }
    }

    @EventListener
    public void on(DeviceOfflineEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            l.onDeviceOffline(e.getDeviceId(), e.getRegisterInfo(), e.getSipTransaction());
        }
    }

    @EventListener
    public void on(DeviceRemoteAddressEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            l.onRemoteAddressChanged(e.getDeviceId(), e.getRemoteAddressInfo());
        }
    }

    // ============ Session listeners ============

    @EventListener
    public void on(DeviceInviteTryingEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onInviteTrying(e.getDeviceId(), e.getCallId());
        }
    }

    @EventListener
    public void on(DeviceInviteOkEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onInviteOk(e.getDeviceId(), e.getCallId());
        }
    }

    @EventListener
    public void on(DeviceInviteFailureEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onInviteFailure(e.getDeviceId(), e.getCallId(), e.getStatusCode());
        }
    }

    @EventListener
    public void on(DeviceAckEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onAck(e.getDeviceId(), e.getCallId(), e.getStatusCode());
        }
    }

    @EventListener
    public void on(DeviceByeEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onBye(e.getDeviceId());
        }
    }

    @EventListener
    public void on(DeviceByeErrorEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onByeError(e.getDeviceId(), e.getErrorMessage());
        }
    }

    @EventListener
    public void on(ServerInviteEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            l.onServerInvite(e.getCallId(), e.getFromUserId(), e.getToUserId(),
                    e.getSessionDescription(), e.getTransactionContextKey());
        }
    }
}
