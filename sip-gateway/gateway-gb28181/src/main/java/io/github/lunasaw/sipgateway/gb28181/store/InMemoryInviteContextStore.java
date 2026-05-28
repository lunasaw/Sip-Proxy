package io.github.lunasaw.sipgateway.gb28181.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 单进程内存版 {@link InviteContextStore}。
 *
 * <p>仅用于：单节点演示、单元测试、本地开发。多节点部署必须替换为 Redis 实现，
 * 否则跨节点 INVITE 回包会路由失败。
 *
 * <p>构造时无条件 {@code log.warn}：业务方多节点部署忘记替换会立即看到告警。
 *
 * @author luna
 */
@Slf4j
public class InMemoryInviteContextStore implements InviteContextStore {

    private final Cache<String, InviteContext> store;

    public InMemoryInviteContextStore(long defaultTtlMs) {
        this.store = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(defaultTtlMs))
                .maximumSize(100_000)
                .build();
        log.warn("InMemoryInviteContextStore active — replace with Redis implementation before multi-node deployment");
    }

    @Override
    public void save(String callId, InviteContext value, long ttlMs) {
        // 单机版本忽略 ttlMs（受 cache 级 expireAfterWrite 控制）
        store.put(callId, value);
    }

    @Override
    public InviteContext find(String callId) {
        return store.getIfPresent(callId);
    }

    @Override
    public void remove(String callId) {
        store.invalidate(callId);
    }
}
