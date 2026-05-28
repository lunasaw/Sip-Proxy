package io.github.lunasaw.sipgateway.core.api;

/**
 * 泛型事务存储基类：跨节点路由用。
 *
 * <p>每协议自定义具体接口，如：
 * <pre>
 * public interface InviteContextStore extends TransactionContextStore&lt;String, InviteContext&gt; {}
 * public record InviteContext(String nodeId, String ctxKey) {}
 * </pre>
 *
 * @param <K> 事务键类型（如 callId / subscriptionId）
 * @param <V> 上下文值类型（如 InviteContext / SubscriptionRef）
 * @author luna
 */
public interface TransactionContextStore<K, V> {
    void save(K key, V value, long ttlMs);

    /**
     * 返回 null 表示不存在（→ 410）；后端故障必须抛 ResponseStatusException(503)
     */
    V find(K key);

    void remove(K key);
}
