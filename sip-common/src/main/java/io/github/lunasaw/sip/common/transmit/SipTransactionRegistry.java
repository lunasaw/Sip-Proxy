package io.github.lunasaw.sip.common.transmit;

import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.TransactionState;
import javax.sip.message.Request;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SIP事务上下文管理器
 * 提供线程安全的事务信息传递和管理能力
 *
 * @author luna
 */
@Slf4j
public class SipTransactionRegistry {

    /**
     * 事务上下文存储
     * Key: CallId + FromTag + CSeq
     * Value: 事务上下文信息
     */
    public static final ConcurrentHashMap<String, TransactionContextInfo> TRANSACTION_CONTEXTS = new ConcurrentHashMap<>();

    /**
     * 读写锁保护事务操作
     */
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 线程本地存储当前事务上下文
     */
    private static final ThreadLocal<TransactionContextInfo> CURRENT_CONTEXT = new ThreadLocal<>();

    /**
     *
     * 事务上下文信息
     */
    @Data
    public static class TransactionContextInfo {
        private final String contextKey;
        private final SipTransaction sipTransaction;
        private final ServerTransaction serverTransaction;
        private final RequestEvent originalEvent;
        private final Request originalRequest;
        private final String traceId;
        private final long createTime;
        private volatile TransactionState lastKnownState;
        private volatile boolean isValid = true;
        private volatile long validUntilOverride = -1L;

        public TransactionContextInfo(RequestEvent requestEvent, ServerTransaction serverTransaction) {
            this.originalEvent = requestEvent;
            this.originalRequest = requestEvent.getRequest();
            this.serverTransaction = serverTransaction;
            this.contextKey = generateContextKey(originalRequest);
            this.sipTransaction = extractSipTransaction(originalRequest);
            this.traceId = TraceContext.traceId();
            this.createTime = System.currentTimeMillis();
            this.lastKnownState = serverTransaction != null ? serverTransaction.getState() : null;
        }

        /**
         * 检查事务是否仍然有效
         */
        public boolean checkAndUpdateValidity() {
            if (!isValid) {
                return false;
            }

            if (serverTransaction != null) {
                TransactionState currentState = serverTransaction.getState();
                this.lastKnownState = currentState;

                // 检查事务状态是否有效
                if (currentState == TransactionState.TERMINATED ||
                        currentState == TransactionState.COMPLETED) {
                    this.isValid = false;
                    log.debug("事务状态无效: contextKey={}, state={}", contextKey, currentState);
                    return false;
                }
            }

            // 检查事务超时（默认 SIP 标准 32 秒，业务方可通过 extendValidity 续期）
            long now = System.currentTimeMillis();
            long deadline = (validUntilOverride > 0) ? validUntilOverride : (createTime + 32000);
            if (now > deadline) {
                this.isValid = false;
                log.debug("事务超时: contextKey={}, deadline={}", contextKey, deadline);
                return false;
            }

            return isValid;
        }

        /**
         * 续期上下文有效期到当前时间之后 ttlMs 毫秒。
         *
         * <p>注意：JAIN-SIP 服务端事务在 Proceeding 状态没有内置 Timer C，
         * 但 INVITE 客户端事务受 RFC 3261 Timer B 约束（默认 32s），
         * 续期超过 30s 仅对"取回 RequestEvent 异步回包"有意义，
         * 设备侧仍可能因 Timer B 超时而拒收 200 OK。
         */
        public void extendValidity(long ttlMs) {
            this.validUntilOverride = System.currentTimeMillis() + ttlMs;
        }

        /**
         * 手动标记事务为无效
         */
        public void invalidate() {
            this.isValid = false;
        }
    }

    /**
     * 创建并存储事务上下文
     */
    public static TransactionContextInfo createContext(RequestEvent requestEvent, ServerTransaction serverTransaction) {
        TransactionContextInfo context = new TransactionContextInfo(requestEvent, serverTransaction);

        rwLock.writeLock().lock();
        try {
            TRANSACTION_CONTEXTS.put(context.getContextKey(), context);
            CURRENT_CONTEXT.set(context);
            log.debug("创建事务上下文: key={}, traceId={}", context.getContextKey(), context.getTraceId());
            return context;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 获取事务上下文
     */
    public static TransactionContextInfo getContext(String contextKey) {
        rwLock.readLock().lock();
        try {
            TransactionContextInfo context = TRANSACTION_CONTEXTS.get(contextKey);
            if (context != null && !context.checkAndUpdateValidity()) {
                // 如果上下文无效，移除它
                removeContext(contextKey);
                return null;
            }
            return context;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 获取当前线程的事务上下文
     */
    public static TransactionContextInfo getCurrentContext() {
        TransactionContextInfo context = CURRENT_CONTEXT.get();
        if (context != null && !context.checkAndUpdateValidity()) {
            CURRENT_CONTEXT.remove();
            return null;
        }
        return context;
    }

    /**
     * 设置当前线程的事务上下文
     */
    public static void setCurrentContext(TransactionContextInfo context) {
        CURRENT_CONTEXT.set(context);
    }

    /**
     * 传递事务上下文到新线程
     */
    public static void propagateContextToThread(String contextKey) {
        TransactionContextInfo context = getContext(contextKey);
        if (context != null) {
            CURRENT_CONTEXT.set(context);
            // 传递TraceId
            if (context.getTraceId() != null) {
                try {
                    // 这里可以设置TraceContext，具体实现依赖于你的链路追踪框架
                    log.debug("传递事务上下文到线程: key={}, traceId={}", contextKey, context.getTraceId());
                } catch (Exception e) {
                    log.warn("传递TraceId失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 续期事务上下文有效期。
     *
     * @param contextKey 上下文键
     * @param ttlMs      从当前时刻起的有效毫秒数
     * @return true 续期成功；false 上下文不存在或已无效，无法续期
     */
    public static boolean extendContext(String contextKey, long ttlMs) {
        rwLock.readLock().lock();
        try {
            TransactionContextInfo ctx = TRANSACTION_CONTEXTS.get(contextKey);
            if (ctx == null || !ctx.checkAndUpdateValidity()) {
                return false;
            }
            ctx.extendValidity(ttlMs);
            log.debug("续期事务上下文: key={}, ttlMs={}", contextKey, ttlMs);
            return true;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 移除事务上下文
     */
    public static void removeContext(String contextKey) {
        rwLock.writeLock().lock();
        try {
            TransactionContextInfo removed = TRANSACTION_CONTEXTS.remove(contextKey);
            if (removed != null) {
                removed.invalidate();
                log.debug("移除事务上下文: key={}", contextKey);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 清理当前线程的上下文
     */
    public static void clearCurrentContext() {
        CURRENT_CONTEXT.remove();
    }

    /**
     * 清理过期的事务上下文
     */
    public static void cleanupExpiredContexts() {
        rwLock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            TRANSACTION_CONTEXTS.entrySet().removeIf(entry -> {
                TransactionContextInfo context = entry.getValue();
                // 清理超过5分钟的上下文
                if (now - context.getCreateTime() > 300000 || !context.checkAndUpdateValidity()) {
                    context.invalidate();
                    log.debug("清理过期事务上下文: key={}", entry.getKey());
                    return true;
                }
                return false;
            });
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 获取事务上下文统计信息
     */
    public static String getContextStats() {
        rwLock.readLock().lock();
        try {
            int total = TRANSACTION_CONTEXTS.size();
            long valid = TRANSACTION_CONTEXTS.values().stream()
                    .mapToLong(ctx -> ctx.checkAndUpdateValidity() ? 1 : 0)
                    .sum();
            return String.format("总上下文数: %d, 有效上下文数: %d", total, valid);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 生成上下文键
     */
    private static String generateContextKey(Request request) {
        try {
            String callId = request.getHeader("Call-ID").toString().split(":")[1].trim();
            String fromTag = null;
            String cseq = request.getHeader("CSeq").toString().split(":")[1].trim();

            // 尝试获取From标签
            String fromHeader = request.getHeader("From").toString();
            if (fromHeader.contains("tag=")) {
                fromTag = fromHeader.substring(fromHeader.indexOf("tag=") + 4);
                int semicolon = fromTag.indexOf(';');
                if (semicolon != -1) {
                    fromTag = fromTag.substring(0, semicolon);
                }
            }

            return callId + "_" + (fromTag != null ? fromTag : "notag") + "_" + cseq;
        } catch (Exception e) {
            log.warn("生成上下文键失败，使用默认键: {}", e.getMessage());
            return "unknown_" + System.currentTimeMillis();
        }
    }

    /**
     * 提取SIP事务信息
     */
    private static SipTransaction extractSipTransaction(Request request) {
        SipTransaction transaction = new SipTransaction();
        try {
            // 提取Call-ID
            String callIdHeader = request.getHeader("Call-ID").toString();
            if (callIdHeader.contains(":")) {
                transaction.setCallId(callIdHeader.split(":", 2)[1].trim());
            }

            // 提取From Tag
            String fromHeader = request.getHeader("From").toString();
            if (fromHeader.contains("tag=")) {
                String fromTag = fromHeader.substring(fromHeader.indexOf("tag=") + 4);
                int semicolon = fromTag.indexOf(';');
                if (semicolon != -1) {
                    fromTag = fromTag.substring(0, semicolon);
                }
                transaction.setFromTag(fromTag);
            }

            // 提取To Tag (如果存在)
            String toHeader = request.getHeader("To").toString();
            if (toHeader.contains("tag=")) {
                String toTag = toHeader.substring(toHeader.indexOf("tag=") + 4);
                int semicolon = toTag.indexOf(';');
                if (semicolon != -1) {
                    toTag = toTag.substring(0, semicolon);
                }
                transaction.setToTag(toTag);
            }

            // 提取Via Branch
            String viaHeader = request.getHeader("Via").toString();
            if (viaHeader.contains("branch=")) {
                String viaBranch = viaHeader.substring(viaHeader.indexOf("branch=") + 7);
                int semicolon = viaBranch.indexOf(';');
                if (semicolon != -1) {
                    viaBranch = viaBranch.substring(0, semicolon);
                }
                transaction.setViaBranch(viaBranch);
            }

        } catch (Exception e) {
            log.warn("提取SIP事务信息失败: {}", e.getMessage());
        }
        return transaction;
    }
}