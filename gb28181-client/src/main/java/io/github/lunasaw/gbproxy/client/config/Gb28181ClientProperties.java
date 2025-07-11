package io.github.lunasaw.gbproxy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

/**
 * GB28181客户端配置属性类 - 支持外部化配置
 *
 * @author luna
 * @date 2024/1/6
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.gb28181.client")
public class Gb28181ClientProperties {

    /**
     * 心跳间隔
     */
    private Duration keepAliveInterval = Duration.ofMinutes(1);

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试延迟
     */
    private Duration retryDelay = Duration.ofSeconds(5);

    /**
     * 注册有效期（秒）
     */
    private int registerExpires = 3600;

    /**
     * 客户端ID
     */
    private String clientId = "34020000001320000001";

    /**
     * 客户端名称
     */
    private String clientName = "GB28181-Client";

    /**
     * 用户名
     */
    private String username = "admin";

    /**
     * 密码
     */
    private String password = "123456";

    /**
     * 性能配置
     */
    private Performance performance = new Performance();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 应用启动时自动验证配置
     */
    @PostConstruct
    public void validateOnStartup() {
        validate();
    }

    @Data
    public static class Performance {
        /**
         * 消息队列大小
         */
        private int messageQueueSize = 1000;

        /**
         * 线程池大小
         */
        private int threadPoolSize = 200;

        /**
         * 是否启用监控
         */
        private boolean enableMetrics = true;

        /**
         * 是否启用异步处理
         */
        private boolean enableAsync = true;

        /**
         * 批处理大小
         */
        private int batchSize = 100;

        /**
         * 处理超时时间（毫秒）
         */
        private long processingTimeoutMs = 5000;

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThresholdMs = 100;
    }

    @Data
    public static class Cache {
        /**
         * 设备缓存最大大小
         */
        private int deviceMaxSize = 50000;

        /**
         * 设备缓存过期时间
         */
        private Duration deviceExpireAfterWrite = Duration.ofHours(2);

        /**
         * 设备缓存访问后过期时间
         */
        private Duration deviceExpireAfterAccess = Duration.ofMinutes(30);

        /**
         * 订阅缓存最大大小
         */
        private int subscribeMaxSize = 5000;

        /**
         * 订阅缓存过期时间
         */
        private Duration subscribeExpireAfterWrite = Duration.ofMinutes(5);

        /**
         * 订阅缓存访问后过期时间
         */
        private Duration subscribeExpireAfterAccess = Duration.ofMinutes(2);

        /**
         * 事务缓存最大大小
         */
        private int transactionMaxSize = 2000;

        /**
         * 事务缓存过期时间
         */
        private Duration transactionExpireAfterWrite = Duration.ofMinutes(1);

        /**
         * 消息缓存最大大小
         */
        private int messageMaxSize = 10000;

        /**
         * 消息缓存过期时间
         */
        private Duration messageExpireAfterWrite = Duration.ofMinutes(30);

        /**
         * 消息缓存访问后过期时间
         */
        private Duration messageExpireAfterAccess = Duration.ofMinutes(15);

        /**
         * 是否启用缓存统计
         */
        private boolean enableStats = true;
    }

    /**
     * 是否启用了监控
     */
    public boolean isMetricsEnabled() {
        return performance.enableMetrics;
    }

    /**
     * 是否启用了异步处理
     */
    public boolean isAsyncEnabled() {
        return performance.enableAsync;
    }

    /**
     * 获取线程池配置参数
     */
    public ThreadPoolConfig getThreadPoolConfig() {
        return new ThreadPoolConfig(
                performance.threadPoolSize,
                performance.messageQueueSize,
                performance.processingTimeoutMs);
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolConfig {
        private final int poolSize;
        private final int queueSize;
        private final long timeoutMs;

        public ThreadPoolConfig(int poolSize, int queueSize, long timeoutMs) {
            this.poolSize = poolSize;
            this.queueSize = queueSize;
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative: " + maxRetries);
        }

        if (performance.threadPoolSize <= 0) {
            throw new IllegalArgumentException("Thread pool size must be positive: " + performance.threadPoolSize);
        }

        if (performance.messageQueueSize <= 0) {
            throw new IllegalArgumentException("Message queue size must be positive: " + performance.messageQueueSize);
        }

        if (cache.deviceMaxSize <= 0) {
            throw new IllegalArgumentException("Device cache max size must be positive: " + cache.deviceMaxSize);
        }
    }
}