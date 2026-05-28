package io.github.lunasaw.sipgateway.gb28181.store;

import io.github.lunasaw.sipgateway.core.api.TransactionContextStore;

/**
 * GB28181 INVITE 事务上下文存储。
 *
 * <p>对应 LAYERED-ARCHITECTURE.md §5.3 方案 A：以 callId 为键存
 * (nodeId, ctxKey) 二元组，让业务侧只感知 callId 即可路由回正确节点。
 *
 * <p>生产环境必须落 Redis Sentinel/Cluster。本接口的默认实现
 * {@link InMemoryInviteContextStore} 仅用于单机演示与单测。
 *
 * @author luna
 */
public interface InviteContextStore extends TransactionContextStore<String, InviteContext> {
}
