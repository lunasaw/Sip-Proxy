package io.github.lunasaw.sip.common.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * SIP性能监控指标 - 使用Micrometer收集性能数据
 *
 * @author luna
 * @date 2024/1/6
 */
@Slf4j
@Component
public class SipMetrics {

    private final MeterRegistry              meterRegistry;
    private final Counter                    messageProcessedCounter;
    private final Timer                      messageProcessingTimer;
    private final Gauge                      activeDevicesGauge;
    private final Counter                    errorCounter;
    private final Timer                      requestTimer;
    private final Timer                      responseTimer;
    private final Timer timeoutTimer;
    private final Gauge                      queueSizeGauge;

    // 标记是否启用监控
    private final boolean                    metricsEnabled;

    // 实时统计数据
    private final AtomicInteger              activeDeviceCount = new AtomicInteger(0);
    private final AtomicInteger              currentQueueSize  = new AtomicInteger(0);
    private final Map<String, AtomicInteger> methodCounters    = new ConcurrentHashMap<>();

    public SipMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricsEnabled = meterRegistry != null;

        if (metricsEnabled) {
            // 消息处理计数器
            this.messageProcessedCounter = Counter.builder("sip.messages.processed")
                .description("Total processed SIP messages")
                .register(meterRegistry);

            // 消息处理时间
            this.messageProcessingTimer = Timer.builder("sip.message.processing.time")
                .description("SIP message processing time")
                .register(meterRegistry);

            // 活跃设备数量
            this.activeDevicesGauge = Gauge.builder("sip.devices.active", this, SipMetrics::getActiveDeviceCount)
                .description("Number of active devices")
                .register(meterRegistry);

            // 错误计数器
            this.errorCounter = Counter.builder("sip.errors")
                .description("Total SIP processing errors")
                .register(meterRegistry);

            // 请求处理时间
            this.requestTimer = Timer.builder("sip.request.processing.time")
                .description("SIP request processing time")
                .register(meterRegistry);

            // 响应处理时间
            this.responseTimer = Timer.builder("sip.response.processing.time")
                .description("SIP response processing time")
                .register(meterRegistry);

            // 超时处理时间
            this.timeoutTimer = Timer.builder("sip.timeout.processing.time")
                    .description("SIP timeout processing time")
                    .register(meterRegistry);

            // 队列大小
            this.queueSizeGauge = Gauge.builder("sip.queue.size", this, SipMetrics::getCurrentQueueSize)
                .description("Current message queue size")
                .register(meterRegistry);

            log.info("SIP metrics initialized with MeterRegistry");
        } else {
            // 创建空的指标对象，避免NPE
            this.messageProcessedCounter = null;
            this.messageProcessingTimer = null;
            this.activeDevicesGauge = null;
            this.errorCounter = null;
            this.requestTimer = null;
            this.responseTimer = null;
            this.timeoutTimer = null;
            this.queueSizeGauge = null;
            log.warn("SIP metrics initialized without MeterRegistry - metrics will be disabled");
        }
    }

    /**
     * 记录消息处理完成
     */
    public void recordMessageProcessed() {
        if (metricsEnabled && messageProcessedCounter != null) {
            messageProcessedCounter.increment();
        }
    }

    /**
     * 记录消息处理完成（带标签）
     */
    public void recordMessageProcessed(String method, String status) {
        if (metricsEnabled && meterRegistry != null) {
            Counter.builder("sip.messages.processed")
                .tag("method", method)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 开始计时
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    /**
     * 记录消息处理时间
     */
    public void recordProcessingTime(Timer.Sample sample) {
        if (metricsEnabled && messageProcessingTimer != null && sample != null) {
            sample.stop(messageProcessingTimer);
        }
    }

    /**
     * 记录请求处理时间
     */
    public void recordRequestProcessingTime(Timer.Sample sample) {
        if (metricsEnabled && requestTimer != null && sample != null) {
            sample.stop(requestTimer);
        }
    }

    /**
     * 记录响应处理时间
     */
    public void recordResponseProcessingTime(Timer.Sample sample) {
        if (metricsEnabled && responseTimer != null && sample != null) {
            sample.stop(responseTimer);
        }
    }

    /**
     * 记录超时处理时间
     */
    public void recordTimeoutProcessingTime(Timer.Sample sample) {
        if (metricsEnabled && timeoutTimer != null && sample != null) {
            sample.stop(timeoutTimer);
        }
    }

    /**
     * 记录错误
     */
    public void recordError() {
        if (metricsEnabled && errorCounter != null) {
            errorCounter.increment();
        }
    }

    /**
     * 记录错误（带标签）
     */
    public void recordError(String errorType, String method) {
        if (metricsEnabled && meterRegistry != null) {
            Counter.builder("sip.errors")
                .tag("error_type", errorType)
                .tag("method", method)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 增加活跃设备数
     */
    public void incrementActiveDevices() {
        activeDeviceCount.incrementAndGet();
    }

    /**
     * 减少活跃设备数
     */
    public void decrementActiveDevices() {
        activeDeviceCount.decrementAndGet();
    }

    /**
     * 设置活跃设备数
     */
    public void setActiveDeviceCount(int count) {
        activeDeviceCount.set(count);
    }

    /**
     * 获取活跃设备数
     */
    public int getActiveDeviceCount() {
        return activeDeviceCount.get();
    }

    /**
     * 更新队列大小
     */
    public void updateQueueSize(int size) {
        currentQueueSize.set(size);
    }

    /**
     * 获取当前队列大小
     */
    public int getCurrentQueueSize() {
        return currentQueueSize.get();
    }

    /**
     * 记录特定方法的调用次数
     */
    public void recordMethodCall(String method) {
        if (metricsEnabled && meterRegistry != null) {
            methodCounters.computeIfAbsent(method, k -> {
                AtomicInteger counter = new AtomicInteger(0);
                Gauge.builder("sip.method.calls", counter, AtomicInteger::get)
                    .tag("method", method)
                    .description("Number of calls for SIP method: " + method)
                    .register(meterRegistry);
                return counter;
            }).incrementAndGet();
        }
    }

    /**
     * 记录消息大小
     */
    public void recordMessageSize(long size) {
        if (metricsEnabled && meterRegistry != null) {
            DistributionSummary.builder("sip.message.size")
                .description("SIP message size in bytes")
                .register(meterRegistry)
                .record(size);
        }
    }

    /**
     * 记录网络延迟
     */
    public void recordNetworkLatency(long latencyMs) {
        if (metricsEnabled && meterRegistry != null) {
            Timer.builder("sip.network.latency")
                .description("Network latency for SIP messages")
                .register(meterRegistry)
                .record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 创建一个自定义计时器
     */
    public Timer createCustomTimer(String name, String description) {
        if (metricsEnabled && meterRegistry != null) {
            return Timer.builder(name)
                .description(description)
                .register(meterRegistry);
        }
        return null;
    }

    /**
     * 创建一个自定义计数器
     */
    public Counter createCustomCounter(String name, String description) {
        if (metricsEnabled && meterRegistry != null) {
            return Counter.builder(name)
                .description(description)
                .register(meterRegistry);
        }
        return null;
    }

    /**
     * 获取所有监控指标的摘要
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== SIP Metrics Summary ===\n");
        summary.append("Active Devices: ").append(getActiveDeviceCount()).append("\n");
        summary.append("Messages Processed: ").append(messageProcessedCounter.count()).append("\n");
        summary.append("Errors: ").append(errorCounter.count()).append("\n");
        summary.append("Current Queue Size: ").append(getCurrentQueueSize()).append("\n");
        summary.append("Average Processing Time: ").append(messageProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)).append("ms\n");
        summary.append("Average Request Time: ").append(requestTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)).append("ms\n");
        summary.append("Average Response Time: ").append(responseTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)).append("ms\n");

        if (!methodCounters.isEmpty()) {
            summary.append("Method Call Counts:\n");
            methodCounters.forEach((method, counter) -> summary.append("  ").append(method).append(": ").append(counter.get()).append("\n"));
        }

        return summary.toString();
    }

    /**
     * 重置所有计数器（用于测试）
     */
    public void resetCounters() {
        activeDeviceCount.set(0);
        currentQueueSize.set(0);
        methodCounters.clear();
        log.info("SIP metrics counters reset");
    }
}