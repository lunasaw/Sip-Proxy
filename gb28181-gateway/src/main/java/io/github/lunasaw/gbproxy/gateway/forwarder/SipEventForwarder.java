package io.github.lunasaw.gbproxy.gateway.forwarder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceSessionListener;
import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 协议层 → 业务层的 listener 转发器。
 *
 * <p>对应 LAYERED-ARCHITECTURE.md §6.3：
 * <ul>
 *   <li>注册：可选写入 {@link DeviceSessionCache}（业务方提供时），通知业务上线</li>
 *   <li>设备主动 INVITE：写入 {@link InviteContextStore} 供跨节点回包路由，按 callId 幂等推送</li>
 *   <li>告警：直接转推</li>
 * </ul>
 *
 * <p>{@code sessionCache} 为 null 时跳过缓存写入，业务方可在 {@link BusinessNotifier#deviceOnline} 中自行处理。
 *
 * @author luna
 */
@Slf4j
@RequiredArgsConstructor
public class SipEventForwarder implements DeviceLifecycleListener, DeviceNotifyListener, DeviceSessionListener {

    private final GatewayProperties gatewayProperties;
    private final InviteContextStore inviteContextStore;
    /** 可选：业务方提供时在注册事件中写入设备会话缓存 */
    private final DeviceSessionCache sessionCache;
    private final BusinessNotifier businessNotifier;

    private Cache<String, Boolean> processedInvites;

    @PostConstruct
    public void initIdempotencyCache() {
        long windowMs = gatewayProperties.getInviteIdempotencyWindowMs();
        this.processedInvites = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(windowMs))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public void onDeviceRegister(String deviceId, RegisterInfo info) {
        if (sessionCache != null && info != null) {
            // DeviceSessionCache 接口只有 getToDevice，写入由业务方自行扩展；
            // 此处仅作为扩展点预留，业务方可覆盖此 Bean 或在 BusinessNotifier 中处理
        }
        businessNotifier.deviceOnline(deviceId, info);
    }

    @Override
    public void onServerInvite(String callId, String fromUserId, String toUserId,
                               String rawSdp,
                               GbSessionDescription sessionDescription, String transactionContextKey) {
        // UDP 下设备会按 T1 退避重传 INVITE，按 callId 幂等避免向业务侧重复推送
        Boolean prev = processedInvites.asMap().putIfAbsent(callId, Boolean.TRUE);
        if (prev != null) {
            log.debug("INVITE 重传，跳过重复推送: callId={}", callId);
            return;
        }

        inviteContextStore.save(callId,
                gatewayProperties.getNodeId(),
                transactionContextKey,
                gatewayProperties.getInviteContextTtlMs());

        businessNotifier.inviteIncoming(callId, fromUserId, toUserId, rawSdp, sessionDescription, transactionContextKey);
    }

    @Override
    public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {
        businessNotifier.alarm(deviceId, notify);
    }

    /** 仅供单元测试访问（按 callId 取幂等闸门状态）。 */
    Cache<String, Boolean> getProcessedInvites() {
        return processedInvites;
    }
}
