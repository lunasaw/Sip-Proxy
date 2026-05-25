package io.github.lunasaw.sip.common.transmit;

import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.TransactionState;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;

/**
 * 事务感知的响应命令管理器
 * 扩展原有ResponseCmd，提供事务上下文感知的响应发送能力
 *
 * @author luna
 */
@Slf4j
public class TransactionAwareResponseCmd {

    /**
     * 事务感知的响应构建器
     */
    public static class TransactionAwareSipResponseBuilder extends ResponseCmd.SipResponseBuilder {
        private boolean useContextTransaction = true;
        private String contextKey;

        public TransactionAwareSipResponseBuilder(int statusCode) {
            super(statusCode);
        }

        /**
         * 设置是否使用上下文事务
         */
        public TransactionAwareSipResponseBuilder useContextTransaction(boolean useContextTransaction) {
            this.useContextTransaction = useContextTransaction;
            return this;
        }

        /**
         * 设置上下文键
         */
        public TransactionAwareSipResponseBuilder contextKey(String contextKey) {
            this.contextKey = contextKey;
            return this;
        }

        /**
         * 重写构建并发送响应方法
         */
        @Override
        public void send() {
            try {
                // 优先尝试使用事务上下文
                if (useContextTransaction) {
                    if (sendWithTransactionContext()) {
                        return; // 成功发送，直接返回
                    }
                    // 如果上下文事务失败，继续使用原有逻辑
                    log.warn("使用事务上下文发送响应失败，降级到原有逻辑");
                }

                // 使用原有逻辑
                super.send();

            } catch (Exception e) {
                log.error("事务感知发送SIP响应失败", e);
                throw new RuntimeException("事务感知发送SIP响应失败", e);
            }
        }

        /**
         * 使用事务上下文发送响应
         */
        private boolean sendWithTransactionContext() {
            try {
                // 获取当前线程的事务上下文
                TransactionContextInfo context = SipTransactionRegistry.getCurrentContext();
                if (context == null && contextKey != null) {
                    // 如果当前线程没有上下文，尝试根据键获取
                    context = SipTransactionRegistry.getContext(contextKey);
                }

                if (context == null) {
                    log.debug("未找到事务上下文，无法使用上下文事务发送响应");
                    return false;
                }

                if (!context.checkAndUpdateValidity()) {
                    log.debug("事务上下文无效: key={}", context.getContextKey());
                    return false;
                }

                ServerTransaction transaction = context.getServerTransaction();
                if (transaction == null) {
                    log.debug("事务上下文中没有ServerTransaction: key={}", context.getContextKey());
                    return false;
                }

                // 验证事务状态
                TransactionState state = transaction.getState();
                if (state == TransactionState.TERMINATED || state == TransactionState.COMPLETED) {
                    log.debug("事务状态无效: key={}, state={}", context.getContextKey(), state);
                    context.invalidate();
                    return false;
                }

                // 构建响应
                Response response = buildResponseFromContext(context);

                // 使用事务发送响应
                transaction.sendResponse(response);
                log.debug("使用事务上下文成功发送响应: key={}, status={}",
                        context.getContextKey(), response.getStatusCode());

                return true;

            } catch (Exception e) {
                log.warn("使用事务上下文发送响应异常: {}", e.getMessage());
                return false;
            }
        }

        /**
         * 从事务上下文构建响应
         */
        private Response buildResponseFromContext(TransactionContextInfo context) throws Exception {
            // 使用原始请求构建响应
            return super.buildResponse();
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 创建事务感知的响应构建器
     */
    public static TransactionAwareSipResponseBuilder response(int statusCode) {
        return new TransactionAwareSipResponseBuilder(statusCode);
    }

    /**
     * 事务感知的快速响应发送（自动使用当前上下文）
     */
    public static void sendResponse(int statusCode) {
        response(statusCode)
                .useContextTransaction(true)
                .send();
    }

    /**
     * 事务感知的快速响应发送（带短语）
     */
    public static void sendResponse(int statusCode, String phrase) {
        response(statusCode)
                .useContextTransaction(true)
                .phrase(phrase)
                .send();
    }

    /**
     * 事务感知的快速响应发送（带内容）
     */
    public static void sendResponse(int statusCode, String content, ContentTypeHeader contentTypeHeader) {
        response(statusCode)
                .useContextTransaction(true)
                .content(content)
                .contentType(contentTypeHeader)
                .send();
    }

    /**
     * 使用指定上下文键发送响应
     */
    public static void sendResponseWithContext(int statusCode, String contextKey) {
        response(statusCode)
                .contextKey(contextKey)
                .useContextTransaction(true)
                .send();
    }

    /**
     * 使用指定上下文键发送带短语的响应
     */
    public static void sendResponseWithContext(int statusCode, String phrase, String contextKey) {
        response(statusCode)
                .useContextTransaction(true)
                .contextKey(contextKey)
                .phrase(phrase)
                .send();
    }

    /**
     * 发送200 OK响应（事务感知）
     */
    public static void sendOK() {
        sendResponse(Response.OK, "OK");
    }

    /**
     * 发送400 Bad Request响应（事务感知）
     */
    public static void sendBadRequest() {
        sendResponse(Response.BAD_REQUEST, "Bad Request");
    }

    /**
     * 发送500 Internal Server Error响应（事务感知）
     */
    public static void sendInternalServerError() {
        sendResponse(Response.SERVER_INTERNAL_ERROR, "Internal Server Error");
    }

    // ==================== 兼容性方法 ====================

    /**
     * 兼容原有API的响应发送（增强版）
     */
    public static void sendResponseSafe(int statusCode, RequestEvent requestEvent, ServerTransaction serverTransaction) {
        try {
            // 优先尝试使用事务感知方式
            if (SipTransactionRegistry.getCurrentContext() != null) {
                sendResponse(statusCode);
                return;
            }

            // 降级到原有方式
            ResponseCmd.sendResponse(statusCode, requestEvent, serverTransaction);
        } catch (Exception e) {
            log.error("发送响应失败，尝试无事务模式", e);
            try {
                ResponseCmd.sendResponseNoTransaction(statusCode, requestEvent);
            } catch (Exception fallbackException) {
                log.error("无事务模式发送响应也失败", fallbackException);
                throw new RuntimeException("发送SIP响应失败", e);
            }
        }
    }

    /**
     * 兼容原有API的带短语响应发送（增强版）
     */
    public static void sendResponseSafe(int statusCode, String phrase, RequestEvent requestEvent, ServerTransaction serverTransaction) {
        try {
            // 优先尝试使用事务感知方式
            if (SipTransactionRegistry.getCurrentContext() != null) {
                sendResponse(statusCode, phrase);
                return;
            }

            // 降级到原有方式
            ResponseCmd.sendResponse(statusCode, phrase, requestEvent, serverTransaction);
        } catch (Exception e) {
            log.error("发送响应失败，尝试无事务模式", e);
            try {
                ResponseCmd.sendResponseNoTransaction(statusCode, phrase, requestEvent);
            } catch (Exception fallbackException) {
                log.error("无事务模式发送响应也失败", fallbackException);
                throw new RuntimeException("发送SIP响应失败", e);
            }
        }
    }

    // ==================== 诊断和监控方法 ====================

    /**
     * 检查当前线程是否有有效的事务上下文
     */
    public static boolean hasValidTransactionContext() {
        TransactionContextInfo context = SipTransactionRegistry.getCurrentContext();
        return context != null && context.checkAndUpdateValidity();
    }

    /**
     * 获取当前事务上下文信息
     */
    public static String getCurrentTransactionInfo() {
        TransactionContextInfo context = SipTransactionRegistry.getCurrentContext();
        if (context == null) {
            return "无事务上下文";
        }

        return String.format("上下文键: %s, 有效性: %s, 事务状态: %s, 创建时间: %d",
                context.getContextKey(),
                context.isValid(),
                context.getLastKnownState(),
                context.getCreateTime());
    }

    /**
     * 强制刷新当前事务上下文状态
     */
    public static boolean refreshCurrentTransactionContext() {
        TransactionContextInfo context = SipTransactionRegistry.getCurrentContext();
        if (context != null) {
            return context.checkAndUpdateValidity();
        }
        return false;
    }
}