package io.github.lunasaw.gbproxy.test.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceSessionListener;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 协议层 → 业务层的 listener 转发器（v1.5.x：从 @EventListener Device*Event 形态迁移到 listener 接口）。
 *
 * <p>对应 LAYERED-ARCHITECTURE.md §6.3：
 * <ul>
 *   <li>注册：写入 {@code DeviceSessionCache}（NAT IP 切换时先 DEL 再 SET，由 {@link SipBusinessConfig#register} 覆盖语义保证），通知业务上线</li>
 *   <li>设备主动 INVITE：写入 {@link InviteContextStore} 供跨节点回包路由，按 callId 幂等推送</li>
 *   <li>告警：直接转推</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SipEventForwarder implements DeviceLifecycleListener, DeviceNotifyListener, DeviceSessionListener {

    private final GatewayProperties gatewayProperties;
    private final InviteContextStore inviteContextStore;
    private final SipBusinessConfig sessionCache;
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
        if (info != null) {
            sessionCache.register(deviceId, info.getRemoteIp(), info.getRemotePort(),
                    info.getTransport() == null ? "UDP" : info.getTransport());
        }
        businessNotifier.deviceOnline(deviceId, info);
    }

    @Override
    public void onServerInvite(String callId, String fromUserId, String toUserId,
                               GbSessionDescription sessionDescription, String transactionContextKey) {
        // UDP 下设备会按 T1 退避重传 INVITE，框架按相同 contextKey 覆盖写入安全，
        // 但 ServerSessionEvent.SERVER_INVITE 会被多次发布，按 callId 幂等避免向业务侧重复推送。
        Boolean prev = processedInvites.asMap().putIfAbsent(callId, Boolean.TRUE);
        if (prev != null) {
            log.debug("INVITE 重传，跳过重复推送: callId={}", callId);
            return;
        }

        inviteContextStore.save(callId,
                gatewayProperties.getNodeId(),
                transactionContextKey,
                gatewayProperties.getInviteContextTtlMs());

        businessNotifier.inviteIncoming(callId, fromUserId, toUserId, sessionDescription, transactionContextKey);
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
