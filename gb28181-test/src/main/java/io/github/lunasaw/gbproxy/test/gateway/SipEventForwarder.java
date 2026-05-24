package io.github.lunasaw.gbproxy.test.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAlarmEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 协议层 → 业务层的 Spring Event 转发器。
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
public class SipEventForwarder {

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

    @EventListener
    public void onRegister(DeviceRegisterEvent event) {
        RegisterInfo info = event.getRegisterInfo();
        if (info != null) {
            sessionCache.register(event.getDeviceId(), info.getRemoteIp(), info.getRemotePort(),
                    info.getTransport() == null ? "UDP" : info.getTransport());
        }
        businessNotifier.deviceOnline(event);
    }

    @EventListener
    public void onServerInvite(ServerInviteEvent event) {
        // UDP 下设备会按 T1 退避重传 INVITE，框架按相同 contextKey 覆盖写入安全，
        // 但 ServerInviteEvent 会被多次发布，按 callId 幂等避免向业务侧重复推送。
        Boolean prev = processedInvites.asMap().putIfAbsent(event.getCallId(), Boolean.TRUE);
        if (prev != null) {
            log.debug("INVITE 重传，跳过重复推送: callId={}", event.getCallId());
            return;
        }

        inviteContextStore.save(event.getCallId(),
                gatewayProperties.getNodeId(),
                event.getTransactionContextKey(),
                gatewayProperties.getInviteContextTtlMs());

        businessNotifier.inviteIncoming(event);
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent event) {
        businessNotifier.alarm(event);
    }
}
