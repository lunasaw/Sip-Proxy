package io.github.lunasaw.sip.common.context;

import javax.sip.RequestEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * SIP事务上下文管理器
 * 通过ThreadLocal管理完整的SIP事务上下文，包括Call-ID、CSeq、From/To头部等
 * 确保请求-响应-ACK整个链路使用相同的事务参数
 * <p>
 * 支持两种模式：
 * 1. 显式模式：通过setRequestEvent直接设置原始请求事件，自动提取事务参数
 * 2. 隐式模式：通过setTransactionInfo设置完整事务信息，在消息处理链路中自动传递
 *
 * @author luna
 * @date 2024/08/11
 */
@Slf4j
public class SipTransactionContext {

    /**
     * 线程本地存储原始请求事件（显式模式）
     */
    private static final ThreadLocal<RequestEvent> REQUEST_EVENT_HOLDER = new ThreadLocal<>();

    /**
     * 线程本地存储完整事务信息（隐式模式）
     */
    private static final ThreadLocal<SipTransactionInfo> TRANSACTION_INFO_HOLDER = new ThreadLocal<>();

    /**
     * 线程本地存储Call-ID（向后兼容）
     */
    private static final ThreadLocal<String> CALL_ID_HOLDER = new ThreadLocal<>();

    /**
     * 线程本地存储事务类型标识
     */
    private static final ThreadLocal<TransactionType> TRANSACTION_TYPE_HOLDER = new ThreadLocal<>();

    /**
     * SIP事务完整信息
     * 包含事务匹配所需的所有关键信息
     */
    @Data
    public static class SipTransactionInfo {
        /**
         * Call-ID头部
         */
        private String callId;
        /**
         * CSeq头部（序列号和方法）
         */
        private Long cSeq;
        /**
         * CSeq方法
         */
        private String method;
        /**
         * From头部
         */
        private String fromHeader;
        /**
         * To头部
         */
        private String toHeader;
        /**
         * From标签
         */
        private String fromTag;
        /**
         * To标签
         */
        private String toTag;

        /**
         * 从RequestEvent提取事务信息
         */
        public static SipTransactionInfo fromRequestEvent(RequestEvent requestEvent) {
            if (requestEvent == null) {
                return null;
            }

            try {
                Request request = requestEvent.getRequest();
                SipTransactionInfo info = new SipTransactionInfo();

                // 提取Call-ID
                CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
                if (callIdHeader != null) {
                    info.setCallId(callIdHeader.getCallId());
                }

                // 提取CSeq
                CSeqHeader cSeqHeader = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
                if (cSeqHeader != null) {
                    info.setCSeq(cSeqHeader.getSeqNumber());
                    info.setMethod(cSeqHeader.getMethod());
                }

                // 提取From头部
                FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
                if (fromHeader != null) {
                    info.setFromHeader(fromHeader.toString());
                    info.setFromTag(fromHeader.getTag());
                }

                // 提取To头部
                ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
                if (toHeader != null) {
                    info.setToHeader(toHeader.toString());
                    info.setToTag(toHeader.getTag());
                }

                return info;
            } catch (Exception e) {
                log.warn("从RequestEvent提取事务信息失败", e);
                return null;
            }
        }

        @Override
        public String toString() {
            return String.format("SipTransactionInfo{callId='%s', cSeq=%d %s, from='%s', to='%s'}",
                    callId, cSeq, method, fromHeader, toHeader);
        }
    }

    /**
     * 事务类型枚举
     */
    public enum TransactionType {
        /**
         * 显式事务：基于原始RequestEvent
         */
        EXPLICIT,
        /**
         * 隐式事务：基于ThreadLocal传递的Call-ID
         */
        IMPLICIT
    }

    // ==================== 显式模式方法 ====================

    /**
     * 设置原始请求事件（显式模式）
     * 自动提取完整的事务信息
     *
     * @param requestEvent 原始请求事件
     */
    public static void setRequestEvent(RequestEvent requestEvent) {
        if (requestEvent != null) {
            REQUEST_EVENT_HOLDER.set(requestEvent);
            TRANSACTION_TYPE_HOLDER.set(TransactionType.EXPLICIT);

            // 提取完整事务信息
            SipTransactionInfo transactionInfo = SipTransactionInfo.fromRequestEvent(requestEvent);
            if (transactionInfo != null) {
                TRANSACTION_INFO_HOLDER.set(transactionInfo);
                // 向后兼容：同时设置Call-ID
                CALL_ID_HOLDER.set(transactionInfo.getCallId());
                log.debug("设置SIP事务上下文（显式模式）: {}", transactionInfo);
            } else {
                log.warn("无法从RequestEvent提取事务信息");
            }
        }
    }

    /**
     * 获取原始请求事件
     *
     * @return 原始请求事件，如果不存在则返回null
     */
    public static RequestEvent getRequestEvent() {
        return REQUEST_EVENT_HOLDER.get();
    }

    // ==================== 隐式模式方法 ====================

    /**
     * 设置完整事务信息（隐式模式）
     * 通常在AbstractSipListener中自动调用
     *
     * @param transactionInfo 完整事务信息
     */
    public static void setTransactionInfo(SipTransactionInfo transactionInfo) {
        if (transactionInfo != null) {
            TRANSACTION_INFO_HOLDER.set(transactionInfo);
            // 如果没有显式设置RequestEvent，则使用隐式模式
            if (REQUEST_EVENT_HOLDER.get() == null) {
                TRANSACTION_TYPE_HOLDER.set(TransactionType.IMPLICIT);
            }
            // 向后兼容：同时设置Call-ID
            if (transactionInfo.getCallId() != null) {
                CALL_ID_HOLDER.set(transactionInfo.getCallId());
            }
            log.debug("设置SIP事务上下文（隐式模式）: {}", transactionInfo);
        }
    }

    /**
     * 设置Call-ID（隐式模式 - 向后兼容）
     * 在消息处理器中自动调用，无需业务代码干预
     *
     * @param callId Call-ID
     */
    public static void setCallId(String callId) {
        if (callId != null && !callId.trim().isEmpty()) {
            CALL_ID_HOLDER.set(callId.trim());
            // 如果没有显式设置RequestEvent，则使用隐式模式
            if (REQUEST_EVENT_HOLDER.get() == null) {
                TRANSACTION_TYPE_HOLDER.set(TransactionType.IMPLICIT);
            }
            log.debug("设置SIP事务上下文（隐式Call-ID模式）: callId={}", callId);
        }
    }

    /**
     * 获取当前线程的完整事务信息
     * 优先级：显式模式的RequestEvent > 隐式模式的TransactionInfo
     *
     * @return 完整事务信息，如果不存在则返回null
     */
    public static SipTransactionInfo getCurrentTransactionInfo() {
        // 优先从显式模式获取
        RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
        if (requestEvent != null) {
            SipTransactionInfo info = SipTransactionInfo.fromRequestEvent(requestEvent);
            if (info != null) {
                log.debug("从显式模式获取事务信息: {}", info);
                return info;
            }
        }

        // 从隐式模式获取
        SipTransactionInfo info = TRANSACTION_INFO_HOLDER.get();
        if (info != null) {
            log.debug("从隐式模式获取事务信息: {}", info);
        }
        return info;
    }

    /**
     * 获取当前线程的Call-ID
     * 优先级：显式模式的RequestEvent > 隐式模式的Call-ID
     *
     * @return Call-ID，如果不存在则返回null
     */
    public static String getCurrentCallId() {
        // 优先从显式模式获取
        RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
        if (requestEvent != null) {
            try {
                String callId = requestEvent.getRequest().getHeader("Call-ID").toString();
                log.debug("从显式模式获取Call-ID: {}", callId);
                return callId;
            } catch (Exception e) {
                log.warn("从RequestEvent获取Call-ID失败", e);
            }
        }

        // 从隐式模式获取
        String callId = CALL_ID_HOLDER.get();
        if (callId != null) {
            log.debug("从隐式模式获取Call-ID: {}", callId);
        }
        return callId;
    }

    // ==================== 事务状态查询 ====================

    /**
     * 检查是否存在活跃的SIP事务上下文
     *
     * @return true如果存在活跃的事务上下文
     */
    public static boolean hasActiveTransaction() {
        return REQUEST_EVENT_HOLDER.get() != null || CALL_ID_HOLDER.get() != null;
    }

    /**
     * 获取当前事务类型
     *
     * @return 事务类型，如果不存在则返回null
     */
    public static TransactionType getCurrentTransactionType() {
        return TRANSACTION_TYPE_HOLDER.get();
    }

    /**
     * 检查是否为显式事务模式
     *
     * @return true如果为显式事务模式
     */
    public static boolean isExplicitTransaction() {
        return TransactionType.EXPLICIT.equals(TRANSACTION_TYPE_HOLDER.get());
    }

    /**
     * 检查是否为隐式事务模式
     *
     * @return true如果为隐式事务模式
     */
    public static boolean isImplicitTransaction() {
        return TransactionType.IMPLICIT.equals(TRANSACTION_TYPE_HOLDER.get());
    }

    // ==================== 上下文管理 ====================

    /**
     * 清理当前线程的事务上下文
     * 建议在处理完成后调用，避免内存泄漏
     */
    public static void clear() {
        String callId = getCurrentCallId();
        TransactionType type = TRANSACTION_TYPE_HOLDER.get();

        REQUEST_EVENT_HOLDER.remove();
        TRANSACTION_INFO_HOLDER.remove();
        CALL_ID_HOLDER.remove();
        TRANSACTION_TYPE_HOLDER.remove();

        log.debug("清理SIP事务上下文: callId={}, type={}", callId, type);
    }

    /**
     * 复制事务上下文到新线程
     * 用于异步处理时传递事务上下文
     *
     * @return 事务上下文快照
     */
    public static TransactionSnapshot snapshot() {
        RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
        String callId = CALL_ID_HOLDER.get();
        TransactionType type = TRANSACTION_TYPE_HOLDER.get();

        return new TransactionSnapshot(requestEvent, callId, type);
    }

    /**
     * 从快照恢复事务上下文
     *
     * @param snapshot 事务上下文快照
     */
    public static void restore(TransactionSnapshot snapshot) {
        if (snapshot != null) {
            if (snapshot.getRequestEvent() != null) {
                REQUEST_EVENT_HOLDER.set(snapshot.getRequestEvent());
            }
            if (snapshot.getCallId() != null) {
                CALL_ID_HOLDER.set(snapshot.getCallId());
            }
            if (snapshot.getType() != null) {
                TRANSACTION_TYPE_HOLDER.set(snapshot.getType());
            }
            log.debug("恢复SIP事务上下文: callId={}, type={}", snapshot.getCallId(), snapshot.getType());
        }
    }

    // ==================== 调试和诊断 ====================

    /**
     * 获取当前事务上下文的调试信息
     *
     * @return 调试信息字符串
     */
    public static String getDebugInfo() {
        RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
        String callId = CALL_ID_HOLDER.get();
        TransactionType type = TRANSACTION_TYPE_HOLDER.get();

        StringBuilder sb = new StringBuilder();
        sb.append("SipTransactionContext{");
        sb.append("thread=").append(Thread.currentThread().getName());
        sb.append(", type=").append(type);
        sb.append(", hasRequestEvent=").append(requestEvent != null);
        sb.append(", callId='").append(callId != null ? callId : getCurrentCallId()).append("'");
        sb.append("}");

        return sb.toString();
    }

    /**
     * 事务上下文快照，用于线程间传递
     */
    public static class TransactionSnapshot {
        private final RequestEvent requestEvent;
        private final String callId;
        private final TransactionType type;

        public TransactionSnapshot(RequestEvent requestEvent, String callId, TransactionType type) {
            this.requestEvent = requestEvent;
            this.callId = callId;
            this.type = type;
        }

        public RequestEvent getRequestEvent() {
            return requestEvent;
        }

        public String getCallId() {
            return callId;
        }

        public TransactionType getType() {
            return type;
        }
    }
}