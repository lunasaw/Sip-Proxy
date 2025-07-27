package io.github.lunasaw.sip.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Duration;
import jakarta.annotation.PostConstruct;

/**
 * GB28181通用配置属性类 - 支持外部化配置
 * 包含通用的性能配置和缓存配置，client和server特定配置已拆分到各自模块
 *
 * @author luna
 * @date 2024/1/6
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.gb28181")
public class Gb28181Properties {

    /**
     * 性能配置
     */
    private Performance performance = new Performance();

    /**
     * 缓存配置
     */
    private Cache       cache       = new Cache();

    /**
     * 时间同步配置
     */
    private TimeSync    timeSync    = new TimeSync();

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
        private int     messageQueueSize     = 1000;

        /**
         * 线程池大小
         */
        private int     threadPoolSize       = 200;

        /**
         * 是否启用监控
         */
        private boolean enableMetrics        = true;

        /**
         * 是否启用异步处理
         */
        private boolean enableAsync          = true;

        /**
         * 批处理大小
         */
        private int     batchSize            = 100;

        /**
         * 处理超时时间（毫秒）
         */
        private long    processingTimeoutMs  = 5000;

        /**
         * 慢查询阈值（毫秒）
         */
        private long    slowQueryThresholdMs = 100;
    }

    @Data
    public static class Cache {
        /**
         * 设备缓存最大大小
         */
        private int      deviceMaxSize               = 50000;

        /**
         * 设备缓存过期时间
         */
        private Duration deviceExpireAfterWrite      = Duration.ofHours(2);

        /**
         * 设备缓存访问后过期时间
         */
        private Duration deviceExpireAfterAccess     = Duration.ofMinutes(30);

        /**
         * 订阅缓存最大大小
         */
        private int      subscribeMaxSize            = 5000;

        /**
         * 订阅缓存过期时间
         */
        private Duration subscribeExpireAfterWrite   = Duration.ofMinutes(5);

        /**
         * 订阅缓存访问后过期时间
         */
        private Duration subscribeExpireAfterAccess  = Duration.ofMinutes(2);

        /**
         * 事务缓存最大大小
         */
        private int      transactionMaxSize          = 2000;

        /**
         * 事务缓存过期时间
         */
        private Duration transactionExpireAfterWrite = Duration.ofMinutes(1);

        /**
         * 消息缓存最大大小
         */
        private int      messageMaxSize              = 10000;

        /**
         * 消息缓存过期时间
         */
        private Duration messageExpireAfterWrite     = Duration.ofMinutes(30);

        /**
         * 消息缓存访问后过期时间
         */
        private Duration messageExpireAfterAccess    = Duration.ofMinutes(15);

        /**
         * 是否启用缓存统计
         */
        private boolean  enableStats                 = true;
    }

    /**
     * 时间同步配置
     */
    @Data
    public static class TimeSync {
        /**
         * 是否启用时间同步
         */
        private boolean enabled = true;

        /**
         * 时间同步方式: SIP, NTP, BOTH
         */
        private TimeSyncMode mode = TimeSyncMode.SIP;

        /**
         * 时间偏差阈值（毫秒）
         * 当时间偏差超过此值时会记录警告
         */
        private long offsetThreshold = 1000;

        /**
         * NTP服务器地址
         */
        private String ntpServer = "pool.ntp.org";

        /**
         * NTP校时间隔（秒）
         */
        private long ntpSyncInterval = 3600;

        /**
         * SIP注册过期时间建议值（秒）
         * 当时间偏差超过阈值时的注册过期时间
         */
        private int registerExpireOnError = 36000;

        /**
         * 是否在时间偏差较大时自动调整注册过期时间
         */
        private boolean autoAdjustExpire = true;
    }

    /**
     * 时间同步方式枚举
     */
    public enum TimeSyncMode {
        /**
         * 仅使用SIP校时
         */
        SIP,
        /**
         * 仅使用NTP校时
         */
        NTP,
        /**
         * 同时使用SIP和NTP校时
         */
        BOTH
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
        private final int  poolSize;
        private final int  queueSize;
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