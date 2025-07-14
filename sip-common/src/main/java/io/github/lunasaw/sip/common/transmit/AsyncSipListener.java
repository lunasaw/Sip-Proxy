package io.github.lunasaw.sip.common.transmit;

import io.github.lunasaw.sip.common.utils.TraceUtils;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sip.*;
import javax.sip.message.Response;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步SIP监听器
 * 继承AbstractSipListener，提供异步消息处理能力
 * 使用本地创建的默认线程池实现高性能消息处理
 *
 * @author luna
 */
@Setter
@Getter
@Slf4j
public abstract class AsyncSipListener extends AbstractSipListener {

    private ThreadPoolTaskExecutor messageExecutor;

    public AsyncSipListener() {
        // 创建本地默认线程池
        this.messageExecutor = createDefaultThreadPool();
        log.info("AsyncSipListener初始化完成，使用本地默认线程池");
    }

    /**
     * 创建默认线程池
     * 使用本地创建的线程池，不依赖Spring注入
     *
     * @return 默认线程池
     */
    private ThreadPoolTaskExecutor createDefaultThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 设置线程池参数
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-sip-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 自定义线程工厂
        executor.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "async-sip-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        });

        // 初始化线程池
        executor.initialize();

        log.info("创建本地默认线程池: coreSize={}, maxSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 异步处理RequestEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param requestEvent RequestEvent事件
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

            log.debug("异步处理SIP请求: method={}", method);

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processRequest方法进行实际处理
                    super.processRequest(requestEvent);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed(method, "ASYNC_SUCCESS");
                    }
                } catch (Exception e) {
                    log.error("异步处理请求失败: method={}", method, e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("ASYNC_PROCESSING_ERROR", method);
                        sipMetrics.recordMessageProcessed(method, "ASYNC_ERROR");
                    }
                    // 调用子类异常处理
                    handleRequestException(requestEvent, e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理请求异常: method={}", method, e);
            if (sipMetrics != null) {
                sipMetrics.recordError("ASYNC_DISPATCH_ERROR", method);
                sipMetrics.recordMessageProcessed(method, "ASYNC_DISPATCH_ERROR");
            }
            // 调用子类异常处理
            handleRequestException(requestEvent, e);
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordRequestProcessingTime(sample);
            }
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 异步处理ResponseEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param responseEvent ResponseEvent事件
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

            log.debug("异步处理SIP响应: status={}", status);

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processResponse方法进行实际处理
                    super.processResponse(responseEvent);

                    if (sipMetrics != null) {
                        sipMetrics.recordMessageProcessed("RESPONSE", "ASYNC_SUCCESS");
                    }
                } catch (Exception e) {
                    log.error("异步处理响应失败: status={}", status, e);
                    if (sipMetrics != null) {
                        sipMetrics.recordError("ASYNC_RESPONSE_ERROR", String.valueOf(status));
                        sipMetrics.recordMessageProcessed("RESPONSE", "ASYNC_ERROR");
                    }
                    // 调用子类异常处理
                    handleResponseException(responseEvent, e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理响应异常: status={}", status, e);
            if (sipMetrics != null) {
                sipMetrics.recordError("ASYNC_RESPONSE_DISPATCH_ERROR", String.valueOf(status));
                sipMetrics.recordMessageProcessed("RESPONSE", "ASYNC_DISPATCH_ERROR");
            }
            // 调用子类异常处理
            handleResponseException(responseEvent, e);
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordResponseProcessingTime(sample);
            }
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 异步处理TimeoutEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param timeoutEvent TimeoutEvent事件
     */
    @Override
    @Async("sipMessageProcessor")
    public void processTimeout(TimeoutEvent timeoutEvent) {
        try {
            log.debug("异步处理SIP超时事件");

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processTimeout方法进行实际处理
                    super.processTimeout(timeoutEvent);
                } catch (Exception e) {
                    log.error("异步处理超时事件失败", e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理超时事件异常", e);
        } finally {
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 异步处理IOExceptionEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param exceptionEvent IOExceptionEvent事件
     */
    @Override
    @Async("sipMessageProcessor")
    public void processIOException(IOExceptionEvent exceptionEvent) {
        try {
            log.debug("异步处理SIP IO异常事件");

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processIOException方法进行实际处理
                    super.processIOException(exceptionEvent);
                } catch (Exception e) {
                    log.error("异步处理IO异常事件失败", e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理IO异常事件异常", e);
        } finally {
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 异步处理TransactionTerminatedEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param timeoutEvent TransactionTerminatedEvent事件
     */
    @Override
    @Async("sipMessageProcessor")
    public void processTransactionTerminated(TransactionTerminatedEvent timeoutEvent) {
        try {
            log.debug("异步处理SIP事务终止事件");

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processTransactionTerminated方法进行实际处理
                    super.processTransactionTerminated(timeoutEvent);
                } catch (Exception e) {
                    log.error("异步处理事务终止事件失败", e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理事务终止事件异常", e);
        } finally {
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 异步处理DialogTerminatedEvent事件
     * 重写父类方法，提供异步处理能力
     *
     * @param dialogTerminatedEvent DialogTerminatedEvent事件
     */
    @Override
    @Async("sipMessageProcessor")
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        try {
            log.debug("异步处理SIP会话终止事件");

            String traceId = TraceUtils.getTraceId();
            // 异步调用父类的同步处理逻辑
            messageExecutor.execute(() -> {
                try {
                    TraceUtils.setTraceId(traceId);
                    // 调用父类的processDialogTerminated方法进行实际处理
                    super.processDialogTerminated(dialogTerminatedEvent);
                } catch (Exception e) {
                    log.error("异步处理会话终止事件失败", e);
                } finally {
                    TraceUtils.clearTraceId();
                }
            });

        } catch (Exception e) {
            log.error("异步处理会话终止事件异常", e);
        } finally {
            TraceUtils.clearTraceId();
        }
    }

    /**
     * 获取异步监听器统计信息
     *
     * @return 统计信息
     */
    @Override
    public String getProcessorStats() {
        return String.format("AsyncSipListener[%s, ThreadPoolSize=%d, ActiveThreads=%d]",
                super.getProcessorStats(),
                messageExecutor.getPoolSize(),
                messageExecutor.getActiveCount());
    }

    /**
     * 销毁线程池资源
     * 在应用关闭时调用，确保资源正确释放
     */
    public void destroy() {
        if (messageExecutor != null) {
            log.info("销毁AsyncSipListener本地线程池");
            messageExecutor.shutdown();
        }
    }
}