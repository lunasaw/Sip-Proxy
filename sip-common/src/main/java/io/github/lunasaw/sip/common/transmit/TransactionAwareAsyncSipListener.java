package io.github.lunasaw.sip.common.transmit;

import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.SipTransactionContext.TransactionContextInfo;
import io.github.lunasaw.sip.common.utils.TraceUtils;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sip.*;
import javax.sip.message.Response;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 事务感知的异步SIP监听器
 * 继承AsyncSipListener，增强事务上下文管理能力
 * 确保多线程异步处理时事务信息的正确传递和维护
 *
 * @author luna
 */
@Setter
@Getter
@Slf4j
@Service
public abstract class TransactionAwareAsyncSipListener extends AsyncSipListener {

    /**
     * 事务上下文清理调度器
     */
    private ScheduledThreadPoolExecutor contextCleanupScheduler;

    /**
     * 是否启用事务上下文管理
     */
    private boolean enableTransactionContext = true;

    public TransactionAwareAsyncSipListener() {
        super();
        initializeContextCleanup();
        log.info("TransactionAwareAsyncSipListener初始化完成，启用事务上下文管理");
    }

    /**
     * 初始化上下文清理调度器
     */
    private void initializeContextCleanup() {
        this.contextCleanupScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "sip-context-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        // 每5分钟清理一次过期的事务上下文
        contextCleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                SipTransactionContext.cleanupExpiredContexts();
                log.debug("定期清理事务上下文完成: {}", SipTransactionContext.getContextStats());
            } catch (Exception e) {
                log.error("清理事务上下文失败", e);
            }
        }, 5, 5, TimeUnit.MINUTES);

        log.info("事务上下文清理调度器已启动，清理间隔: 5分钟");
    }

    /**
     * 事务感知的异步请求处理
     * 创建并传递事务上下文到异步线程
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Timer.Sample sample = sipMetrics != null ? sipMetrics.startTimer() : null;
        String method = requestEvent.getRequest().getMethod();

        try {
            // 记录方法调用
            if (sipMetrics != null) {
                sipMetrics.recordMethodCall(method);
            }

            log.debug("事务感知异步处理SIP请求: method={}", method);

            // 创建事务上下文
            TransactionContextInfo transactionContext = null;
            if (enableTransactionContext && !SipLayer.isShuttingDown()) {
                try {
                    ServerTransaction serverTransaction = requestEvent.getServerTransaction();
                    if (serverTransaction == null) {
                        // 尝试获取或创建服务器事务
                        serverTransaction = SipTransactionManager.getServerTransaction(requestEvent.getRequest());
                        if (serverTransaction == null) {
                            log.debug("无法获取服务器事务，可能服务正在关闭，继续处理");
                        }
                    }

                    if (serverTransaction != null) {
                        transactionContext = SipTransactionContext.createContext(requestEvent, serverTransaction);
                        log.debug("创建事务上下文: key={}, method={}", transactionContext.getContextKey(), method);
                    } else {
                        log.debug("服务器事务为空，跳过事务上下文创建，method={}", method);
                    }
                } catch (Exception e) {
                    log.warn("创建事务上下文失败，继续处理: {}", e.getMessage());
                }
            } else if (SipLayer.isShuttingDown()) {
                log.debug("服务正在关闭，跳过事务上下文创建，method={}", method);
            }

            String traceId = TraceUtils.getTraceId();
            final TransactionContextInfo finalContext = transactionContext;

            // 异步执行实际处理逻辑
            getMessageExecutor().execute(() -> {
                TransactionContextInfo currentContext = null;
                try {
                    // 设置TraceId
                    TraceUtils.setTraceId(traceId);

                    // 传递事务上下文到当前线程
                    if (finalContext != null) {
                        SipTransactionContext.setCurrentContext(finalContext);
                        currentContext = finalContext;
                        log.debug("传递事务上下文到异步线程: key={}", finalContext.getContextKey());
                    }

                    // 调用父类的处理逻辑
                    super.processRequest(requestEvent);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed(method, "TRANSACTION_ASYNC_SUCCESS");
                    }

                } catch (Exception e) {
                    log.error("事务感知异步处理请求失败: method={}", method, e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("TRANSACTION_ASYNC_PROCESSING_ERROR", method);
                        sipMetrics.recordMessageProcessed(method, "TRANSACTION_ASYNC_ERROR");
                    }
                    // 调用异常处理
                    handleRequestExceptionWithContext(requestEvent, e, currentContext);
                } finally {
                    // 清理线程本地上下文
                    SipTransactionContext.clearCurrentContext();
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("事务感知异步分发请求异常: method={}", method, e);
            if (sipMetrics != null) {
                sipMetrics.recordError("TRANSACTION_ASYNC_DISPATCH_ERROR", method);
                sipMetrics.recordMessageProcessed(method, "TRANSACTION_ASYNC_DISPATCH_ERROR");
            }
            handleRequestException(requestEvent, e);
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordRequestProcessingTime(sample);
            }
        }
    }

    /**
     * 事务感知的异步响应处理
     */
    @Override
    @Async("sipMessageProcessor")
    public void processResponse(ResponseEvent responseEvent) {
        Timer.Sample sample = sipMetrics != null ? sipMetrics.startTimer() : null;
        Response response = responseEvent.getResponse();
        int status = response.getStatusCode();

        try {
            // 记录方法调用
            if (sipMetrics != null) {
                sipMetrics.recordMethodCall("RESPONSE_" + status);
            }

            log.debug("事务感知异步处理SIP响应: status={}", status);

            String traceId = TraceUtils.getTraceId();

            // 尝试匹配已有的事务上下文
            TransactionContextInfo existingContext = null;
            if (enableTransactionContext) {
                try {
                    // 从响应中提取上下文键信息
                    String contextKey = generateContextKeyFromResponse(response);
                    existingContext = SipTransactionContext.getContext(contextKey);
                    if (existingContext != null) {
                        log.debug("找到匹配的事务上下文: key={}, status={}", contextKey, status);
                    }
                } catch (Exception e) {
                    log.warn("匹配事务上下文失败: {}", e.getMessage());
                }
            }

            final TransactionContextInfo finalContext = existingContext;

            // 异步执行响应处理
            getMessageExecutor().execute(() -> {
                try {
                    // 设置TraceId
                    TraceUtils.setTraceId(traceId);

                    // 传递事务上下文到当前线程
                    if (finalContext != null) {
                        SipTransactionContext.setCurrentContext(finalContext);
                        log.debug("传递事务上下文到响应处理线程: key={}", finalContext.getContextKey());
                    }

                    // 调用父类的响应处理逻辑
                    super.processResponse(responseEvent);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed("RESPONSE_" + status, "TRANSACTION_ASYNC_SUCCESS");
                    }

                } catch (Exception e) {
                    log.error("事务感知异步处理响应失败: status={}", status, e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("TRANSACTION_ASYNC_RESPONSE_ERROR", "RESPONSE_" + status);
                        sipMetrics.recordMessageProcessed("RESPONSE_" + status, "TRANSACTION_ASYNC_ERROR");
                    }
                    handleResponseExceptionWithContext(responseEvent, e, finalContext);
                } finally {
                    // 清理线程本地上下文
                    SipTransactionContext.clearCurrentContext();
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("事务感知异步分发响应异常: status={}", status, e);
            if (sipMetrics != null) {
                sipMetrics.recordError("TRANSACTION_ASYNC_RESPONSE_DISPATCH_ERROR", "RESPONSE_" + status);
            }
            handleResponseException(responseEvent, e);
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordResponseProcessingTime(sample);
            }
        }
    }

    /**
     * 从响应中生成上下文键
     */
    private String generateContextKeyFromResponse(Response response) {
        try {
            String callId = response.getHeader("Call-ID").toString().split(":")[1].trim();
            String fromTag = null;
            String cseq = response.getHeader("CSeq").toString().split(":")[1].trim();

            // 从To头提取tag（响应中的To tag对应请求中的From tag）
            String toHeader = response.getHeader("To").toString();
            if (toHeader.contains("tag=")) {
                fromTag = toHeader.substring(toHeader.indexOf("tag=") + 4);
                int semicolon = fromTag.indexOf(';');
                if (semicolon != -1) {
                    fromTag = fromTag.substring(0, semicolon);
                }
            }

            return callId + "_" + (fromTag != null ? fromTag : "notag") + "_" + cseq;
        } catch (Exception e) {
            log.warn("从响应生成上下文键失败: {}", e.getMessage());
            return "response_unknown_" + System.currentTimeMillis();
        }
    }

    /**
     * 带事务上下文的请求异常处理
     */
    protected void handleRequestExceptionWithContext(RequestEvent requestEvent, Exception e, TransactionContextInfo context) {
        log.error("请求处理异常，上下文: {}", context != null ? context.getContextKey() : "无", e);
        // 子类可以重写此方法提供特定的异常处理逻辑
        handleRequestException(requestEvent, e);
    }

    /**
     * 带事务上下文的响应异常处理
     */
    protected void handleResponseExceptionWithContext(ResponseEvent responseEvent, Exception e, TransactionContextInfo context) {
        log.error("响应处理异常，上下文: {}", context != null ? context.getContextKey() : "无", e);
        // 子类可以重写此方法提供特定的异常处理逻辑
        handleResponseException(responseEvent, e);
    }

    /**
     * 响应异常处理（原方法不存在，添加默认实现）
     */
    protected void handleResponseException(ResponseEvent responseEvent, Exception e) {
        log.error("响应处理异常: status={}", responseEvent.getResponse().getStatusCode(), e);
        // 子类可以重写此方法
    }

    /**
     * 获取事务上下文统计信息
     */
    public String getTransactionContextStats() {
        return SipTransactionContext.getContextStats();
    }

    /**
     * 手动清理事务上下文
     */
    public void cleanupTransactionContexts() {
        SipTransactionContext.cleanupExpiredContexts();
        log.info("手动清理事务上下文完成: {}", getTransactionContextStats());
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        if (contextCleanupScheduler != null && !contextCleanupScheduler.isShutdown()) {
            contextCleanupScheduler.shutdown();
            try {
                if (!contextCleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    contextCleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                contextCleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("事务上下文清理调度器已关闭");
        }
    }
}