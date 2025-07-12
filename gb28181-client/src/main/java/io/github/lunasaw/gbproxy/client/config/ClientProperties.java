package io.github.lunasaw.gbproxy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * GB28181客户端配置属性类 - 支持外部化配置
 *
 * @author luna
 * @date 2024/1/6
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.client")
public class ClientProperties {

    /**
     * 设备认证配置
     */
    @NotNull
    private Auth auth = new Auth();

    /**
     * SIP协议配置
     */
    @NotNull
    private Sip sip = new Sip();

    /**
     * 网络连接配置
     */
    @NotNull
    private Network network = new Network();

    /**
     * 性能配置
     */
    @NotNull
    private Performance performance = new Performance();

    /**
     * 缓存配置
     */
    @NotNull
    private Cache cache = new Cache();

    /**
     * 重试配置
     */
    @NotNull
    private Retry retry = new Retry();

    /**
     * 应用启动时自动验证配置
     */
    @PostConstruct
    public void validateOnStartup() {
        validate();
    }

    /**
     * 设备认证配置
     */
    @Data
    public static class Auth {
        /**
         * 设备ID
         */
        @NotBlank(message = "设备ID不能为空")
        private String deviceId = "34020000001320000001";

        /**
         * 设备名称
         */
        @NotBlank(message = "设备名称不能为空")
        private String deviceName = "GB28181-Client";

        /**
         * 用户名
         */
        @NotBlank(message = "用户名不能为空")
        private String username = "admin";

        /**
         * 密码
         */
        @NotBlank(message = "密码不能为空")
        private String password = "123456";

        /**
         * 注册有效期（秒）
         */
        @Min(value = 60, message = "注册有效期不能少于60秒")
        private int registerExpires = 3600;
    }

    /**
     * SIP协议配置
     */
    @Data
    public static class Sip {
        /**
         * 心跳间隔
         */
        @NotNull(message = "心跳间隔不能为空")
        private Duration keepAliveInterval = Duration.ofMinutes(1);

        /**
         * 本地SIP端口
         */
        @Min(value = 1024, message = "SIP端口不能小于1024")
        private int localPort = 5060;

        /**
         * 本地SIP地址
         */
        @NotBlank(message = "本地SIP地址不能为空")
        private String localAddress = "127.0.0.1";

        /**
         * 远程SIP端口
         */
        @Min(value = 1024, message = "远程SIP端口不能小于1024")
        private int remotePort = 5060;

        /**
         * 远程SIP地址
         */
        @NotBlank(message = "远程SIP地址不能为空")
        private String remoteAddress = "127.0.0.1";

        /**
         * SIP传输协议
         */
        @NotBlank(message = "SIP传输协议不能为空")
        private String transport = "UDP";

        /**
         * 是否启用TLS
         */
        private boolean enableTls = false;

        /**
         * TLS证书路径
         */
        private String tlsCertPath;

        /**
         * TLS私钥路径
         */
        private String tlsKeyPath;
    }

    /**
     * 网络连接配置
     */
    @Data
    public static class Network {
        /**
         * 连接超时时间
         */
        @NotNull(message = "连接超时时间不能为空")
        private Duration connectTimeout = Duration.ofSeconds(10);

        /**
         * 读取超时时间
         */
        @NotNull(message = "读取超时时间不能为空")
        private Duration readTimeout = Duration.ofSeconds(30);

        /**
         * 写入超时时间
         */
        @NotNull(message = "写入超时时间不能为空")
        private Duration writeTimeout = Duration.ofSeconds(10);

        /**
         * 最大连接数
         */
        @Min(value = 1, message = "最大连接数不能小于1")
        private int maxConnections = 100;

        /**
         * 连接池空闲超时时间
         */
        @NotNull(message = "连接池空闲超时时间不能为空")
        private Duration idleTimeout = Duration.ofMinutes(5);

        /**
         * 是否启用连接保活
         */
        private boolean keepAlive = true;

        /**
         * 保活间隔时间
         */
        @NotNull(message = "保活间隔时间不能为空")
        private Duration keepAliveInterval = Duration.ofSeconds(30);
    }

    /**
     * 性能配置
     */
    @Data
    public static class Performance {
        /**
         * 消息队列大小
         */
        @Min(value = 100, message = "消息队列大小不能小于100")
        private int messageQueueSize = 1000;

        /**
         * 线程池核心线程数
         */
        @Min(value = 1, message = "线程池核心线程数不能小于1")
        private int coreThreadSize = 10;

        /**
         * 线程池最大线程数
         */
        @Min(value = 1, message = "线程池最大线程数不能小于1")
        private int maxThreadSize = 200;

        /**
         * 线程池空闲线程存活时间
         */
        @NotNull(message = "线程池空闲线程存活时间不能为空")
        private Duration threadKeepAliveTime = Duration.ofMinutes(1);

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
        @Min(value = 1, message = "批处理大小不能小于1")
        private int batchSize = 100;

        /**
         * 处理超时时间（毫秒）
         */
        @Min(value = 1000, message = "处理超时时间不能小于1000毫秒")
        private long processingTimeoutMs = 5000;

        /**
         * 慢查询阈值（毫秒）
         */
        @Min(value = 10, message = "慢查询阈值不能小于10毫秒")
        private long slowQueryThresholdMs = 100;
    }

    /**
     * 缓存配置
     */
    @Data
    public static class Cache {
        /**
         * 设备缓存最大大小
         */
        @Min(value = 100, message = "设备缓存最大大小不能小于100")
        private int deviceMaxSize = 50000;

        /**
         * 设备缓存过期时间
         */
        @NotNull(message = "设备缓存过期时间不能为空")
        private Duration deviceExpireAfterWrite = Duration.ofHours(2);

        /**
         * 设备缓存访问后过期时间
         */
        @NotNull(message = "设备缓存访问后过期时间不能为空")
        private Duration deviceExpireAfterAccess = Duration.ofMinutes(30);

        /**
         * 订阅缓存最大大小
         */
        @Min(value = 100, message = "订阅缓存最大大小不能小于100")
        private int subscribeMaxSize = 5000;

        /**
         * 订阅缓存过期时间
         */
        @NotNull(message = "订阅缓存过期时间不能为空")
        private Duration subscribeExpireAfterWrite = Duration.ofMinutes(5);

        /**
         * 订阅缓存访问后过期时间
         */
        @NotNull(message = "订阅缓存访问后过期时间不能为空")
        private Duration subscribeExpireAfterAccess = Duration.ofMinutes(2);

        /**
         * 事务缓存最大大小
         */
        @Min(value = 100, message = "事务缓存最大大小不能小于100")
        private int transactionMaxSize = 2000;

        /**
         * 事务缓存过期时间
         */
        @NotNull(message = "事务缓存过期时间不能为空")
        private Duration transactionExpireAfterWrite = Duration.ofMinutes(1);

        /**
         * 消息缓存最大大小
         */
        @Min(value = 100, message = "消息缓存最大大小不能小于100")
        private int messageMaxSize = 10000;

        /**
         * 消息缓存过期时间
         */
        @NotNull(message = "消息缓存过期时间不能为空")
        private Duration messageExpireAfterWrite = Duration.ofMinutes(30);

        /**
         * 消息缓存访问后过期时间
         */
        @NotNull(message = "消息缓存访问后过期时间不能为空")
        private Duration messageExpireAfterAccess = Duration.ofMinutes(15);

        /**
         * 是否启用缓存统计
         */
        private boolean enableStats = true;
    }

    /**
     * 重试配置
     */
    @Data
    public static class Retry {
        /**
         * 最大重试次数
         */
        @Min(value = 0, message = "最大重试次数不能为负数")
        private int maxRetries = 3;

        /**
         * 重试延迟
         */
        @NotNull(message = "重试延迟不能为空")
        private Duration retryDelay = Duration.ofSeconds(5);

        /**
         * 重试延迟倍数
         */
        @Min(value = 1, message = "重试延迟倍数不能小于1")
        private double retryMultiplier = 2.0;

        /**
         * 最大重试延迟
         */
        @NotNull(message = "最大重试延迟不能为空")
        private Duration maxRetryDelay = Duration.ofMinutes(1);

        /**
         * 是否启用指数退避
         */
        private boolean enableExponentialBackoff = true;
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
                performance.coreThreadSize,
                performance.maxThreadSize,
                performance.messageQueueSize,
                performance.threadKeepAliveTime,
                performance.processingTimeoutMs);
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolConfig {
        private final int corePoolSize;
        private final int maxPoolSize;
        private final int queueSize;
        private final Duration keepAliveTime;
        private final long timeoutMs;

        public ThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueSize,
                                Duration keepAliveTime, long timeoutMs) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
            this.keepAliveTime = keepAliveTime;
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        // 验证认证配置
        if (auth.deviceId == null || auth.deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        if (auth.username == null || auth.username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (auth.password == null || auth.password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (auth.registerExpires < 60) {
            throw new IllegalArgumentException("注册有效期不能少于60秒: " + auth.registerExpires);
        }

        // 验证SIP配置
        if (sip.localPort < 1024 || sip.localPort > 65535) {
            throw new IllegalArgumentException("本地SIP端口必须在1024-65535之间: " + sip.localPort);
        }

        if (sip.remotePort < 1024 || sip.remotePort > 65535) {
            throw new IllegalArgumentException("远程SIP端口必须在1024-65535之间: " + sip.remotePort);
        }

        if (sip.keepAliveInterval.toSeconds() < 30) {
            throw new IllegalArgumentException("心跳间隔不能少于30秒: " + sip.keepAliveInterval);
        }

        // 验证网络配置
        if (network.connectTimeout.toSeconds() < 1) {
            throw new IllegalArgumentException("连接超时时间不能少于1秒: " + network.connectTimeout);
        }

        if (network.maxConnections < 1) {
            throw new IllegalArgumentException("最大连接数不能小于1: " + network.maxConnections);
        }

        // 验证性能配置
        if (performance.coreThreadSize > performance.maxThreadSize) {
            throw new IllegalArgumentException("核心线程数不能大于最大线程数: " + performance.coreThreadSize + " > " + performance.maxThreadSize);
        }

        if (performance.messageQueueSize < 100) {
            throw new IllegalArgumentException("消息队列大小不能小于100: " + performance.messageQueueSize);
        }

        if (performance.processingTimeoutMs < 1000) {
            throw new IllegalArgumentException("处理超时时间不能少于1000毫秒: " + performance.processingTimeoutMs);
        }

        // 验证缓存配置
        if (cache.deviceMaxSize < 100) {
            throw new IllegalArgumentException("设备缓存最大大小不能小于100: " + cache.deviceMaxSize);
        }

        if (cache.subscribeMaxSize < 100) {
            throw new IllegalArgumentException("订阅缓存最大大小不能小于100: " + cache.subscribeMaxSize);
        }

        if (cache.transactionMaxSize < 100) {
            throw new IllegalArgumentException("事务缓存最大大小不能小于100: " + cache.transactionMaxSize);
        }

        if (cache.messageMaxSize < 100) {
            throw new IllegalArgumentException("消息缓存最大大小不能小于100: " + cache.messageMaxSize);
        }

        // 验证重试配置
        if (retry.maxRetries < 0) {
            throw new IllegalArgumentException("最大重试次数不能为负数: " + retry.maxRetries);
        }

        if (retry.retryDelay.toSeconds() < 1) {
            throw new IllegalArgumentException("重试延迟不能少于1秒: " + retry.retryDelay);
        }

        if (retry.retryMultiplier < 1.0) {
            throw new IllegalArgumentException("重试延迟倍数不能小于1.0: " + retry.retryMultiplier);
        }

        if (retry.maxRetryDelay.toSeconds() < 10) {
            throw new IllegalArgumentException("最大重试延迟不能少于10秒: " + retry.maxRetryDelay);
        }
    }
}