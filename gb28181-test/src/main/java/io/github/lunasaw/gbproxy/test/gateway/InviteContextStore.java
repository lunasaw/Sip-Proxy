package io.github.lunasaw.gbproxy.test.gateway;

/**
 * INVITE 事务上下文跨节点路由存储。
 *
 * <p>对应 LAYERED-ARCHITECTURE.md §5.3 方案 A：以 {@code callId} 为键存
 * {@code "{nodeId}:{contextKey}"}，让业务侧只感知 callId 即可路由回正确节点。
 *
 * <p>生产环境必须落 Redis Sentinel/Cluster（见 §3 SPOF 警告）。本接口的
 * 默认实现 {@link InMemoryInviteContextStore} 仅用于单机演示与单测。
 */
public interface InviteContextStore {

    /**
     * 写入 callId → 节点+上下文 映射，TTL 到期后自动清理。
     *
     * @param callId    SIP Call-ID
     * @param nodeId    收到 INVITE 的节点
     * @param ctxKey    {@code SipTransactionRegistry} 中的上下文键 ({@code callId_fromTag_cseq})
     * @param ttlMs     映射存活毫秒数
     */
    void save(String callId, String nodeId, String ctxKey, long ttlMs);

    /**
     * 取出 callId 对应的 {@code "{nodeId}:{contextKey}"}，已过期返回 null。
     */
    String find(String callId);

    /**
     * 主动删除——业务回包成功或事务终止时调用，让 callId 复用更可控。
     */
    void remove(String callId);
}
