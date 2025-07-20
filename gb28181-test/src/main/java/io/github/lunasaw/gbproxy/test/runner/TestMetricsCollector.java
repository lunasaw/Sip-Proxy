package io.github.lunasaw.gbproxy.test.runner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试指标收集器
 * 负责收集测试过程中的各种性能指标
 */
@Slf4j
@Component
public class TestMetricsCollector {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> gauges = new ConcurrentHashMap<>();
    private final Map<String, Long> timers = new ConcurrentHashMap<>();
    private boolean collecting = false;
    private String currentSessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 开始收集指标
     */
    public void startCollection(String sessionId) {
        log.info("开始收集测试指标，会话ID: {}", sessionId);

        this.currentSessionId = sessionId;
        this.startTime = LocalDateTime.now();
        this.collecting = true;

        // 初始化基础指标
        initializeMetrics();

        // 启动系统资源监控
        startSystemMonitoring();
    }

    /**
     * 停止收集指标
     */
    public void stopCollection() {
        if (!collecting) {
            return;
        }

        this.endTime = LocalDateTime.now();
        this.collecting = false;

        log.info("停止收集测试指标，会话ID: {}", currentSessionId);

        // 记录最终指标
        recordFinalMetrics();
    }

    /**
     * 初始化指标
     */
    private void initializeMetrics() {
        // 测试计数器
        counters.put("test.total", new AtomicLong(0));
        counters.put("test.success", new AtomicLong(0));
        counters.put("test.failure", new AtomicLong(0));

        // SIP消息计数器
        counters.put("sip.request.sent", new AtomicLong(0));
        counters.put("sip.response.received", new AtomicLong(0));
        counters.put("sip.error", new AtomicLong(0));

        // GB28181消息计数器
        counters.put("gb28181.register", new AtomicLong(0));
        counters.put("gb28181.keepalive", new AtomicLong(0));
        counters.put("gb28181.catalog", new AtomicLong(0));
        counters.put("gb28181.deviceinfo", new AtomicLong(0));
        counters.put("gb28181.invite", new AtomicLong(0));

        // 性能指标
        gauges.put("memory.used.mb", 0.0);
        gauges.put("memory.max.mb", 0.0);
        gauges.put("cpu.usage.percent", 0.0);
        gauges.put("thread.count", 0.0);
    }

    /**
     * 启动系统资源监控
     */
    private void startSystemMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (collecting) {
                try {
                    // 记录内存使用情况
                    Runtime runtime = Runtime.getRuntime();
                    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                    long maxMemory = runtime.maxMemory();

                    gauges.put("memory.used.mb", usedMemory / 1024.0 / 1024.0);
                    gauges.put("memory.max.mb", maxMemory / 1024.0 / 1024.0);

                    // 记录线程数
                    gauges.put("thread.count", (double) Thread.activeCount());

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("系统监控异常", e);
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.setName("test-metrics-monitor");
        monitorThread.start();
    }

    /**
     * 记录最终指标
     */
    private void recordFinalMetrics() {
        if (startTime != null && endTime != null) {
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            timers.put("test.total.duration.ms", durationMs);
        }

        // 计算成功率
        long total = counters.get("test.total").get();
        long success = counters.get("test.success").get();
        if (total > 0) {
            double successRate = (double) success / total * 100;
            gauges.put("test.success.rate.percent", successRate);
        }

        log.info("测试指标收集完成");
        logMetricsSummary();
    }

    /**
     * 记录测试开始
     */
    public void recordTestStart(String testName) {
        if (!collecting) return;

        counters.get("test.total").incrementAndGet();
        timers.put("test." + testName + ".start", System.currentTimeMillis());
    }

    /**
     * 记录测试成功
     */
    public void recordTestSuccess(String testName) {
        if (!collecting) return;

        counters.get("test.success").incrementAndGet();
        recordTestEnd(testName);
    }

    /**
     * 记录测试失败
     */
    public void recordTestFailure(String testName) {
        if (!collecting) return;

        counters.get("test.failure").incrementAndGet();
        recordTestEnd(testName);
    }

    /**
     * 记录测试结束
     */
    private void recordTestEnd(String testName) {
        String startKey = "test." + testName + ".start";
        if (timers.containsKey(startKey)) {
            long startTime = timers.get(startKey);
            long duration = System.currentTimeMillis() - startTime;
            timers.put("test." + testName + ".duration", duration);
            timers.remove(startKey);
        }
    }

    /**
     * 记录SIP请求发送
     */
    public void recordSipRequestSent(String method) {
        if (!collecting) return;

        counters.get("sip.request.sent").incrementAndGet();
        counters.computeIfAbsent("sip.request." + method.toLowerCase(), k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录SIP响应接收
     */
    public void recordSipResponseReceived(int statusCode) {
        if (!collecting) return;

        counters.get("sip.response.received").incrementAndGet();
        String category = getStatusCategory(statusCode);
        counters.computeIfAbsent("sip.response." + category, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录SIP错误
     */
    public void recordSipError(String errorType) {
        if (!collecting) return;

        counters.get("sip.error").incrementAndGet();
        counters.computeIfAbsent("sip.error." + errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录GB28181消息
     */
    public void recordGb28181Message(String messageType) {
        if (!collecting) return;

        String key = "gb28181." + messageType.toLowerCase();
        if (counters.containsKey(key)) {
            counters.get(key).incrementAndGet();
        }
    }

    /**
     * 获取状态码分类
     */
    private String getStatusCategory(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "2xx";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "3xx";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "4xx";
        } else if (statusCode >= 500 && statusCode < 600) {
            return "5xx";
        }
        return "other";
    }

    /**
     * 输出指标摘要
     */
    private void logMetricsSummary() {
        log.info("========================================");
        log.info("          测试指标摘要");
        log.info("========================================");

        // 测试统计
        log.info("测试统计:");
        log.info("  总测试数: {}", counters.get("test.total").get());
        log.info("  成功数: {}", counters.get("test.success").get());
        log.info("  失败数: {}", counters.get("test.failure").get());
        if (gauges.containsKey("test.success.rate.percent")) {
            log.info("  成功率: {:.2f}%", gauges.get("test.success.rate.percent"));
        }

        // SIP统计
        log.info("SIP消息统计:");
        log.info("  请求发送: {}", counters.get("sip.request.sent").get());
        log.info("  响应接收: {}", counters.get("sip.response.received").get());
        log.info("  错误数: {}", counters.get("sip.error").get());

        // GB28181统计
        log.info("GB28181消息统计:");
        log.info("  注册: {}", counters.get("gb28181.register").get());
        log.info("  心跳: {}", counters.get("gb28181.keepalive").get());
        log.info("  目录: {}", counters.get("gb28181.catalog").get());
        log.info("  设备信息: {}", counters.get("gb28181.deviceinfo").get());
        log.info("  邀请: {}", counters.get("gb28181.invite").get());

        // 系统资源
        log.info("系统资源:");
        log.info("  内存使用: {:.2f} MB / {:.2f} MB",
                gauges.get("memory.used.mb"), gauges.get("memory.max.mb"));
        log.info("  线程数: {:.0f}", gauges.get("thread.count"));

        // 执行时间
        if (timers.containsKey("test.total.duration.ms")) {
            log.info("  总执行时间: {} ms", timers.get("test.total.duration.ms"));
        }
    }

    /**
     * 检查是否有指标数据
     */
    public boolean hasMetrics() {
        return !counters.isEmpty() || !gauges.isEmpty() || !timers.isEmpty();
    }

    /**
     * 获取指标的HTML表示
     */
    public String getMetricsHtml() {
        StringBuilder html = new StringBuilder();

        html.append("<h3>测试统计</h3>");
        html.append("<ul>");
        html.append("<li>总测试数: ").append(counters.get("test.total").get()).append("</li>");
        html.append("<li>成功数: ").append(counters.get("test.success").get()).append("</li>");
        html.append("<li>失败数: ").append(counters.get("test.failure").get()).append("</li>");
        if (gauges.containsKey("test.success.rate.percent")) {
            html.append("<li>成功率: ").append(String.format("%.2f%%", gauges.get("test.success.rate.percent"))).append("</li>");
        }
        html.append("</ul>");

        html.append("<h3>SIP消息统计</h3>");
        html.append("<ul>");
        html.append("<li>请求发送: ").append(counters.get("sip.request.sent").get()).append("</li>");
        html.append("<li>响应接收: ").append(counters.get("sip.response.received").get()).append("</li>");
        html.append("<li>错误数: ").append(counters.get("sip.error").get()).append("</li>");
        html.append("</ul>");

        html.append("<h3>系统资源</h3>");
        html.append("<ul>");
        html.append("<li>内存使用: ").append(String.format("%.2f MB / %.2f MB",
                gauges.get("memory.used.mb"), gauges.get("memory.max.mb"))).append("</li>");
        html.append("<li>线程数: ").append(String.format("%.0f", gauges.get("thread.count"))).append("</li>");
        if (timers.containsKey("test.total.duration.ms")) {
            html.append("<li>总执行时间: ").append(timers.get("test.total.duration.ms")).append(" ms</li>");
        }
        html.append("</ul>");

        return html.toString();
    }

    /**
     * 获取计数器值
     */
    public long getCounter(String name) {
        return counters.getOrDefault(name, new AtomicLong(0)).get();
    }

    /**
     * 获取度量值
     */
    public double getGauge(String name) {
        return gauges.getOrDefault(name, 0.0);
    }

    /**
     * 获取计时器值
     */
    public long getTimer(String name) {
        return timers.getOrDefault(name, 0L);
    }
}