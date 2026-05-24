package io.github.lunasaw.gbproxy.server.transmit.event.internal;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UploadSnapShotFinishedNotify;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceResponseListener;
import io.github.lunasaw.gbproxy.server.api.DeviceSessionListener;
import io.github.lunasaw.gbproxy.server.api.dto.DeviceInfoError;
import io.github.lunasaw.gbproxy.server.api.dto.DeviceInfoRequest;
import io.github.lunasaw.gbproxy.server.api.dto.DeviceSubscribeResponse;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerLifecycleEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerNotifyEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Server 端 Listener 分发器：监听 4 个外层 L1 协议事件
 * （{@link ServerQueryResponseEvent} / {@link ServerNotifyEvent} /
 * {@link ServerLifecycleEvent} / {@link ServerSessionEvent}），按 payload 类型分发到
 * 4 个 listener 接口的对应方法。
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有 listener 用 {@code List<>} 注入，全部调用（观察者模式）</li>
 *   <li>无状态单例，业务方 0 个 listener 时事件正常发布但无处理</li>
 *   <li>内部按 payload Java 类型 / 子类型枚举做 typed 分发</li>
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
    public void on(ServerQueryResponseEvent e) {
        Object p = e.getPayload();
        for (DeviceResponseListener l : safe(responseListeners)) {
            if (p instanceof DeviceResponse catalog) {
                l.onCatalogResponse(e.getDeviceId(), e.getSn(), catalog);
            } else if (p instanceof DeviceInfo info) {
                l.onDeviceInfoResponse(e.getDeviceId(), e.getSn(), info);
            } else if (p instanceof DeviceStatus status) {
                l.onDeviceStatusResponse(e.getDeviceId(), e.getSn(), status);
            } else if (p instanceof DeviceRecord record) {
                l.onRecordInfoResponse(e.getDeviceId(), e.getSn(), record);
            } else if (p instanceof PTZPositionResponse resp) {
                l.onPtzPositionResponse(e.getDeviceId(), resp);
            } else if (p instanceof SDCardStatusResponse resp) {
                l.onSdCardStatusResponse(e.getDeviceId(), resp);
            } else if (p instanceof HomePositionResponse resp) {
                l.onHomePositionResponse(e.getDeviceId(), resp);
            } else if (p instanceof CruiseTrackListResponse resp) {
                l.onCruiseTrackListResponse(e.getDeviceId(), resp);
            } else if (p instanceof CruiseTrackResponse resp) {
                l.onCruiseTrackResponse(e.getDeviceId(), resp);
            } else if (p instanceof DeviceConfigResponse resp) {
                l.onConfigResponse(e.getDeviceId(), e.getSn(), resp);
            } else if (p instanceof DeviceSubscribeResponse subRes) {
                l.onSubscribeResponse(e.getDeviceId(), subRes.callId(), subRes.statusCode());
            } else if (p instanceof DeviceOtherUpdateNotify notify) {
                l.onNotifyUpdate(e.getDeviceId(), notify);
            } else if (p instanceof DeviceInfoRequest req) {
                l.onDeviceInfoRequest(e.getDeviceId(), req.content());
            } else if (p instanceof DeviceInfoError err) {
                l.onDeviceInfoError(e.getDeviceId(), err.reason());
            } else {
                log.debug("ServerQueryResponseEvent 未识别 payload: {}", p == null ? "null" : p.getClass().getSimpleName());
            }
        }
    }

    // ============ Notify listeners ============

    @EventListener
    public void on(ServerNotifyEvent e) {
        Object p = e.getPayload();
        for (DeviceNotifyListener l : safe(notifyListeners)) {
            if (p instanceof DeviceAlarmNotify notify) {
                l.onAlarmNotify(e.getDeviceId(), notify);
            } else if (p instanceof DeviceKeepLiveNotify notify) {
                l.onKeepalive(e.getDeviceId(), notify);
            } else if (p instanceof MediaStatusNotify notify) {
                l.onMediaStatus(e.getDeviceId(), notify);
            } else if (p instanceof MobilePositionNotify notify) {
                l.onMobilePositionNotify(e.getDeviceId(), notify);
            } else if (p instanceof UpgradeResultNotify notify) {
                l.onUpgradeResult(e.getDeviceId(), notify);
            } else if (p instanceof UploadSnapShotFinishedNotify notify) {
                l.onSnapShotFinished(e.getDeviceId(), notify);
            } else {
                log.debug("ServerNotifyEvent 未识别 payload: {}", p == null ? "null" : p.getClass().getSimpleName());
            }
        }
    }

    // ============ Lifecycle listeners ============

    @EventListener
    public void on(ServerLifecycleEvent e) {
        for (DeviceLifecycleListener l : safe(lifecycleListeners)) {
            switch (e.getType()) {
                case REGISTER -> l.onDeviceRegister(e.getDeviceId(), e.getRegisterInfo());
                case CHALLENGE -> l.onRegisterChallenge(e.getDeviceId());
                case ONLINE -> l.onDeviceOnline(e.getDeviceId(), e.getSipTransaction());
                case OFFLINE -> l.onDeviceOffline(e.getDeviceId(), e.getRegisterInfo(), e.getSipTransaction());
                case REMOTE_ADDRESS_CHANGED -> l.onRemoteAddressChanged(e.getDeviceId(), e.getRemoteAddressInfo());
            }
        }
    }

    // ============ Session listeners ============

    @EventListener
    public void on(ServerSessionEvent e) {
        for (DeviceSessionListener l : safe(sessionListeners)) {
            switch (e.getType()) {
                case INVITE_TRYING -> l.onInviteTrying(e.getDeviceId(), e.getCallId());
                case INVITE_OK -> l.onInviteOk(e.getDeviceId(), e.getCallId());
                case INVITE_FAILURE -> l.onInviteFailure(e.getDeviceId(), e.getCallId(), e.getStatusCode());
                case ACK -> l.onAck(e.getDeviceId(), e.getCallId(), e.getStatusCode());
                case BYE -> l.onBye(e.getDeviceId());
                case BYE_ERROR -> l.onByeError(e.getDeviceId(), e.getErrorMessage());
                case SERVER_INVITE -> l.onServerInvite(e.getCallId(), e.getFromUserId(), e.getToUserId(),
                        e.getSessionDescription(), e.getTransactionContextKey());
            }
        }
    }
}
