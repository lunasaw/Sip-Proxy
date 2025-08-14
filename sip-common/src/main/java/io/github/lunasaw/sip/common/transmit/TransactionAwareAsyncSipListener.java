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
                log.info("开始定期清理过期事务上下文");
                SipTransactionContext.cleanupExpiredContexts();
                log.info("定期清理事务上下文完成: 清理了过期上下文, 统计信息: {}", SipTransactionContext.getContextStats());
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

            log.info("开始事务感知异步处理SIP请求: method={}, callId={}, traceId={}", method,
                    requestEvent.getRequest().getHeader("Call-ID") != null ?
                            requestEvent.getRequest().getHeader("Call-ID").toString().split(":")[1].trim() : "unknown",
                    TraceUtils.getTraceId());

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
                        log.info("成功创建事务上下文: key={}, method={}, transactionId={}",
                                transactionContext.getContextKey(), method, serverTransaction.getBranchId());
                    } else {
                        log.info("服务器事务为空，跳过事务上下文创建，method={}", method);
                    }
                } catch (Exception e) {
                    log.warn("创建事务上下文失败，继续处理: method={}, error={}", method, e.getMessage(), e);
                }
            } else if (SipLayer.isShuttingDown()) {
                log.info("服务正在关闭，跳过事务上下文创建，method={}", method);
            } else {
                log.info("事务上下文管理已禁用，跳过上下文创建，method={}", method);
            }

            String traceId = TraceUtils.getTraceId();
            final TransactionContextInfo finalContext = transactionContext;

            // 异步执行实际处理逻辑
            log.info("提交请求到异步执行器: method={}, hasContext={}", method, transactionContext != null);
            getMessageExecutor().execute(() -> {
                TransactionContextInfo currentContext = null;
                try {
                    // 设置TraceId
                    TraceUtils.setTraceId(traceId);

                    // 传递事务上下文到当前线程
                    if (finalContext != null) {
                        SipTransactionContext.setCurrentContext(finalContext);
                        currentContext = finalContext;
                        log.info("成功传递事务上下文到异步线程: key={}, threadName={}",
                                finalContext.getContextKey(), Thread.currentThread().getName());
                    } else {
                        log.info("无事务上下文需要传递到异步线程: threadName={}", Thread.currentThread().getName());
                    }

                    // 调用父类的处理逻辑
                    log.info("开始调用父类处理逻辑: method={}", method);
                    super.processRequest(requestEvent);
                    log.info("父类处理逻辑执行完成: method={}", method);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed(method, "TRANSACTION_ASYNC_SUCCESS");
                    }
                    log.info("请求异步处理成功: method={}, traceId={}", method, TraceUtils.getTraceId());

                } catch (Exception e) {
                    log.error("事务感知异步处理请求失败: method={}, contextKey={}, traceId={}",
                            method, currentContext != null ? currentContext.getContextKey() : "none",
                            TraceUtils.getTraceId(), e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("TRANSACTION_ASYNC_PROCESSING_ERROR", method);
                        sipMetrics.recordMessageProcessed(method, "TRANSACTION_ASYNC_ERROR");
                    }
                    // 调用异常处理
                    handleRequestExceptionWithContext(requestEvent, e, currentContext);
                } finally {
                    // 清理线程本地上下文
                    log.info("清理异步线程上下文: method={}, contextKey={}, threadName={}",
                            method, currentContext != null ? currentContext.getContextKey() : "none",
                            Thread.currentThread().getName());
                    SipTransactionContext.clearCurrentContext();
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("事务感知异步分发请求异常: method={}, traceId={}", method, TraceUtils.getTraceId(), e);
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

            log.info("开始事务感知异步处理SIP响应: status={}, callId={}, traceId={}", status,
                    response.getHeader("Call-ID") != null ?
                            response.getHeader("Call-ID").toString().split(":")[1].trim() : "unknown",
                    TraceUtils.getTraceId());

            String traceId = TraceUtils.getTraceId();

            // 尝试匹配已有的事务上下文
            TransactionContextInfo existingContext = null;
            if (enableTransactionContext) {
                try {
                    // 从响应中提取上下文键信息
                    String contextKey = generateContextKeyFromResponse(response);
                    existingContext = SipTransactionContext.getContext(contextKey);
                    if (existingContext != null) {
                        log.info("找到匹配的事务上下文: key={}, status={}, contextAge={}ms",
                                contextKey, status, System.currentTimeMillis() - existingContext.getCreateTime());
                    } else {
                        log.info("未找到匹配的事务上下文: generatedKey={}, status={}", contextKey, status);
                    }
                } catch (Exception e) {
                    log.warn("匹配事务上下文失败: status={}, error={}", status, e.getMessage(), e);
                }
            }

            final TransactionContextInfo finalContext = existingContext;

            // 异步执行响应处理
            log.info("提交响应到异步执行器: status={}, hasContext={}", status, existingContext != null);
            getMessageExecutor().execute(() -> {
                try {
                    // 设置TraceId
                    TraceUtils.setTraceId(traceId);

                    // 传递事务上下文到当前线程
                    if (finalContext != null) {
                        SipTransactionContext.setCurrentContext(finalContext);
                        log.info("成功传递事务上下文到响应处理线程: key={}, threadName={}",
                                finalContext.getContextKey(), Thread.currentThread().getName());
                    } else {
                        log.info("无事务上下文需要传递到响应处理线程: status={}, threadName={}",
                                status, Thread.currentThread().getName());
                    }

                    // 调用父类的响应处理逻辑
                    log.info("开始调用父类响应处理逻辑: status={}", status);
                    super.processResponse(responseEvent);
                    log.info("父类响应处理逻辑执行完成: status={}", status);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed("RESPONSE_" + status, "TRANSACTION_ASYNC_SUCCESS");
                    }
                    log.info("响应异步处理成功: status={}, traceId={}", status, TraceUtils.getTraceId());

                } catch (Exception e) {
                    log.error("事务感知异步处理响应失败: status={}, contextKey={}, traceId={}",
                            status, finalContext != null ? finalContext.getContextKey() : "none",
                            TraceUtils.getTraceId(), e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("TRANSACTION_ASYNC_RESPONSE_ERROR", "RESPONSE_" + status);
                        sipMetrics.recordMessageProcessed("RESPONSE_" + status, "TRANSACTION_ASYNC_ERROR");
                    }
                    handleResponseExceptionWithContext(responseEvent, e, finalContext);
                } finally {
                    // 清理线程本地上下文
                    log.info("清理响应处理线程上下文: status={}, contextKey={}, threadName={}",
                            status, finalContext != null ? finalContext.getContextKey() : "none",
                            Thread.currentThread().getName());
                    SipTransactionContext.clearCurrentContext();
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("事务感知异步分发响应异常: status={}, traceId={}", status, TraceUtils.getTraceId(), e);
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
            log.debug("开始从响应生成上下文键: status={}", response.getStatusCode());
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

            String contextKey = callId + "_" + (fromTag != null ? fromTag : "notag") + "_" + cseq;
            log.debug("成功生成上下文键: callId={}, fromTag={}, cseq={}, contextKey={}",
                    callId, fromTag, cseq, contextKey);
            return contextKey;
        } catch (Exception e) {
            log.warn("从响应生成上下文键失败: status={}, error={}", response.getStatusCode(), e.getMessage(), e);
            String fallbackKey = "response_unknown_" + System.currentTimeMillis();
            log.info("使用备用上下文键: {}", fallbackKey);
            return fallbackKey;
        }
    }

    /**
     * 带事务上下文的请求异常处理
     */
    protected void handleRequestExceptionWithContext(RequestEvent requestEvent, Exception e, TransactionContextInfo context) {
        String method = requestEvent.getRequest().getMethod();
        log.error("请求处理异常，method={}, 上下文: {}, traceId={}",
                method, context != null ? context.getContextKey() : "无", TraceUtils.getTraceId(), e);
        // 子类可以重写此方法提供特定的异常处理逻辑
        handleRequestException(requestEvent, e);
    }

    /**
     * 带事务上下文的响应异常处理
     */
    protected void handleResponseExceptionWithContext(ResponseEvent responseEvent, Exception e, TransactionContextInfo context) {
        int status = responseEvent.getResponse().getStatusCode();
        log.error("响应处理异常，status={}, 上下文: {}, traceId={}",
                status, context != null ? context.getContextKey() : "无", TraceUtils.getTraceId(), e);
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
        log.info("开始手动清理事务上下文");
        SipTransactionContext.cleanupExpiredContexts();
        log.info("手动清理事务上下文完成: 清理了过期上下文, 统计信息: {}", getTransactionContextStats());
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        log.info("开始关闭TransactionAwareAsyncSipListener");
        if (contextCleanupScheduler != null && !contextCleanupScheduler.isShutdown()) {
            log.info("关闭事务上下文清理调度器");
            contextCleanupScheduler.shutdown();
            try {
                if (!contextCleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    contextCleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待调度器关闭时被中断，强制关闭");
                contextCleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("事务上下文清理调度器已关闭");
        } else {
            log.info("事务上下文清理调度器已经关闭或未初始化");
        }
        log.info("TransactionAwareAsyncSipListener关闭完成");
    }
}