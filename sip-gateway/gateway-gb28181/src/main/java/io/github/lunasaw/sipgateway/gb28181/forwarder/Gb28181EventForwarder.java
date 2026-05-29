package io.github.lunasaw.sipgateway.gb28181.forwarder;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceResponseListener;
import io.github.lunasaw.gbproxy.server.api.DeviceSessionListener;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayProperties;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContext;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * GB28181 协议 → 业务层的 listener 转发器。
 *
 * <p>实现 4 个 listener × 35 个事件方法，每方法 1~2 行 emit。
 * <ul>
 *   <li>Lifecycle: 5 个（注册/挑战/在线/离线/地址变更）</li>
 *   <li>Notify: 7 个（告警/心跳/媒体状态/位置/升级/抓拍/视频上传）</li>
 *   <li>Session: 7 个（INVITE 三向握手/BYE/ACK/ServerInvite）</li>
 *   <li>Response: 16 个（Catalog/DeviceInfo/Status/RecordInfo/PTZ/...）</li>
 * </ul>
 *
 * <p>关键不变量：
 * <ul>
 *   <li>ServerInvite 按 callId 幂等去重（UDP 重传场景）</li>
 *   <li>ServerInvite 写入 InviteContextStore 供跨节点回包路由</li>
 *   <li>所有 emit 走 BusinessNotifier，必须异步避免阻塞 SIP 事件线程</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
@RequiredArgsConstructor
public class Gb28181EventForwarder
        implements DeviceLifecycleListener, DeviceNotifyListener,
        DeviceSessionListener, DeviceResponseListener {

    private final BusinessNotifier notifier;
    private final InviteContextStore inviteContextStore;
    private final GatewayProperties coreProps;
    private final Gb28181GatewayProperties gb28181Props;

    private Cache<String, Boolean> processedInvites;

    @PostConstruct
    public void initIdempotencyCache() {
        long windowMs = gb28181Props.getInviteIdempotencyWindowMs();
        this.processedInvites = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(windowMs))
                .maximumSize(10_000)
                .build();
    }

    private void emit(String type, String deviceId, String correlationId, Object payload) {
        Map<String, Object> map;
        if (payload instanceof Map) {
            map = (Map<String, Object>) payload;
        } else if (payload == null) {
            map = new HashMap<>();
        } else {
            String json = JSON.toJSONString(payload);
            map = JSON.parseObject(json, Map.class);
        }
        notifier.notify(new GatewayEvent(
                type, deviceId, correlationId,
                System.currentTimeMillis(),
                map,
                coreProps.getNodeId()));
    }

    // ========== Lifecycle (5) ==========

    @Override
    public void onDeviceRegister(String deviceId, RegisterInfo info) {
        emit("gb28181.Lifecycle.Register", deviceId, null, info);
    }

    @Override
    public void onRegisterChallenge(String deviceId) {
        emit("gb28181.Lifecycle.RegisterChallenge", deviceId, null, Map.of());
    }

    @Override
    public void onDeviceOnline(String deviceId, SipTransaction transaction) {
        emit("gb28181.Lifecycle.Online", deviceId, null, transaction);
    }

    @Override
    public void onDeviceOffline(String deviceId, RegisterInfo info, SipTransaction transaction) {
        emit("gb28181.Lifecycle.Offline", deviceId, null,
                Map.of("registerInfo", info != null ? info : Map.of(),
                        "transaction", transaction != null ? transaction : Map.of()));
    }

    @Override
    public void onRemoteAddressChanged(String deviceId, RemoteAddressInfo info) {
        emit("gb28181.Lifecycle.RemoteAddressChanged", deviceId, null, info);
    }

    // ========== Notify (7) ==========

    @Override
    public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {
        emit("gb28181.Notify.Alarm", deviceId, null, notify);
    }

    @Override
    public void onKeepalive(String deviceId, DeviceKeepLiveNotify notify) {
        emit("gb28181.Notify.Keepalive", deviceId, null, notify);
    }

    @Override
    public void onMediaStatus(String deviceId, MediaStatusNotify notify) {
        emit("gb28181.Notify.MediaStatus", deviceId, null, notify);
    }

    @Override
    public void onMobilePositionNotify(String deviceId, MobilePositionNotify notify) {
        emit("gb28181.Notify.MobilePosition", deviceId, null, notify);
    }

    @Override
    public void onUpgradeResult(String deviceId, UpgradeResultNotify notify) {
        emit("gb28181.Notify.UpgradeResult", deviceId, null, notify);
    }

    @Override
    public void onSnapShotFinished(String deviceId, UploadSnapShotFinishedNotify notify) {
        emit("gb28181.Notify.SnapShotFinished", deviceId, null, notify);
    }

    @Override
    public void onVideoUploadNotify(String deviceId, VideoUploadNotify notify) {
        emit("gb28181.Notify.VideoUpload", deviceId, null, notify);
    }

    // ========== Session (7) ==========

    @Override
    public void onInviteTrying(String deviceId, String callId) {
        emit("gb28181.Session.InviteTrying", deviceId, callId, Map.of());
    }

    @Override
    public void onInviteOk(String deviceId, String callId) {
        emit("gb28181.Session.InviteOk", deviceId, callId, Map.of());
    }

    @Override
    public void onInviteFailure(String deviceId, String callId, int statusCode) {
        emit("gb28181.Session.InviteFailure", deviceId, callId, Map.of("statusCode", statusCode));
    }

    @Override
    public void onAck(String deviceId, String callId, int statusCode) {
        emit("gb28181.Session.Ack", deviceId, callId, Map.of("statusCode", statusCode));
    }

    @Override
    public void onBye(String deviceId) {
        emit("gb28181.Session.Bye", deviceId, null, Map.of());
    }

    @Override
    public void onByeError(String deviceId, String errorMessage) {
        emit("gb28181.Session.ByeError", deviceId, null, Map.of("error", errorMessage));
    }

    @Override
    public void onServerInvite(String callId, String fromUserId, String toUserId,
                               String rawSdp, GbSessionDescription sdp,
                               String transactionContextKey) {
        // INVITE 幂等：UDP 重传按 callId 在窗口内去重
        Boolean prev = processedInvites.asMap().putIfAbsent(callId, Boolean.TRUE);
        if (prev != null) {
            log.debug("INVITE 重传，跳过重复推送: callId={}", callId);
            return;
        }

        // 写入跨节点路由
        inviteContextStore.save(callId,
                new InviteContext(coreProps.getNodeId(), transactionContextKey),
                gb28181Props.getInviteContextTtlMs());

        Map<String, Object> payload = new HashMap<>();
        payload.put("fromUserId", fromUserId);
        payload.put("toUserId", toUserId);
        payload.put("rawSdp", rawSdp);
        payload.put("sdp", sdp);
        payload.put("ctxKey", transactionContextKey);
        emit("gb28181.Session.ServerInvite", null, callId, payload);
    }

    // ========== Response (16) ==========

    @Override
    public void onCatalogResponse(String deviceId, String sn, DeviceResponse catalog) {
        emit("gb28181.Response.Catalog", deviceId, sn, catalog);
    }

    @Override
    public void onDeviceInfoResponse(String deviceId, String sn, DeviceInfo info) {
        emit("gb28181.Response.DeviceInfo", deviceId, sn, info);
    }

    @Override
    public void onDeviceInfoError(String deviceId, String reason) {
        emit("gb28181.Response.DeviceInfoError", deviceId, null, Map.of("reason", reason));
    }

    @Override
    public void onDeviceInfoRequest(String deviceId, String content) {
        emit("gb28181.Response.DeviceInfoRequest", deviceId, null, Map.of("content", content));
    }

    @Override
    public void onDeviceStatusResponse(String deviceId, String sn, DeviceStatus status) {
        emit("gb28181.Response.DeviceStatus", deviceId, sn, status);
    }

    @Override
    public void onRecordInfoResponse(String deviceId, String sn, DeviceRecord record) {
        emit("gb28181.Response.RecordInfo", deviceId, sn, record);
    }

    @Override
    public void onPtzPositionResponse(String deviceId, PTZPositionResponse response) {
        emit("gb28181.Response.PtzPosition", deviceId, null, response);
    }

    @Override
    public void onSdCardStatusResponse(String deviceId, SDCardStatusResponse response) {
        emit("gb28181.Response.SdCardStatus", deviceId, null, response);
    }

    @Override
    public void onHomePositionResponse(String deviceId, HomePositionResponse response) {
        emit("gb28181.Response.HomePosition", deviceId, null, response);
    }

    @Override
    public void onCruiseTrackListResponse(String deviceId, CruiseTrackListResponse response) {
        emit("gb28181.Response.CruiseTrackList", deviceId, null, response);
    }

    @Override
    public void onCruiseTrackResponse(String deviceId, CruiseTrackResponse response) {
        emit("gb28181.Response.CruiseTrack", deviceId, null, response);
    }

    @Override
    public void onConfigResponse(String deviceId, String sn, DeviceConfigResponse response) {
        emit("gb28181.Response.Config", deviceId, sn, response);
    }

    @Override
    public void onConfigDownloadResponse(String deviceId, DeviceConfigDownloadResponse response) {
        emit("gb28181.Response.ConfigDownload", deviceId, null, response);
    }

    @Override
    public void onPresetQueryResponse(String deviceId, PresetQueryResponse response) {
        emit("gb28181.Response.PresetQuery", deviceId, null, response);
    }

    @Override
    public void onSubscribeResponse(String deviceId, String callId, int statusCode) {
        emit("gb28181.Response.Subscribe", deviceId, callId, Map.of("statusCode", statusCode));
    }

    @Override
    public void onNotifyUpdate(String deviceId, DeviceOtherUpdateNotify notify) {
        emit("gb28181.Response.NotifyUpdate", deviceId, null, notify);
    }

    /** 仅供单元测试访问。 */
    public Cache<String, Boolean> getProcessedInvites() {
        return processedInvites;
    }
}
