package io.github.lunasaw.gbproxy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * GB28181客户端配置属性类 - 简化版
 * 提供核心配置项，其他配置使用合理默认值
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
     * 性能配置
     */
    @NotNull
    private Performance performance = new Performance();

    /**
     * 应用启动时验证核心配置
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
        private int registerExpires = 3600;
    }

    /**
     * SIP协议配置
     */
    @Data
    public static class Sip {
        /**
         * 本地SIP端口
         */
        private int localPort = 5060;

        /**
         * 本地SIP地址
         */
        private String localAddress = "127.0.0.1";

        /**
         * 远程SIP端口
         */
        private int remotePort = 5060;

        /**
         * 远程SIP地址
         */
        @NotBlank(message = "远程SIP地址不能为空")
        private String remoteAddress = "127.0.0.1";

        /**
         * SIP传输协议
         */
        private String transport = "UDP";

        /**
         * 心跳间隔
         */
        private Duration keepAliveInterval = Duration.ofMinutes(1);

        /**
         * 连接超时时间
         */
        private Duration connectTimeout = Duration.ofSeconds(10);

        /**
         * 是否启用TLS
         */
        private boolean enableTls = false;
    }

    /**
     * 性能配置
     */
    @Data
    public static class Performance {
        /**
         * 线程池核心线程数
         */
        private int coreThreadSize = 10;

        /**
         * 线程池最大线程数
         */
        private int maxThreadSize = 50;

        /**
         * 消息队列大小
         */
        private int messageQueueSize = 1000;

        /**
         * 是否启用异步处理
         */
        private boolean enableAsync = true;

        /**
         * 是否启用监控
         */
        private boolean enableMetrics = true;

        /**
         * 处理超时时间（毫秒）
         */
        private long processingTimeoutMs = 5000;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 重试延迟
         */
        private Duration retryDelay = Duration.ofSeconds(2);
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
                Duration.ofMinutes(1),
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
     * 验证核心配置的有效性
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

        // 验证SIP基础配置
        if (sip.remoteAddress == null || sip.remoteAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("远程SIP地址不能为空");
        }

        // 验证端口范围
        if (sip.localPort < 1024 || sip.localPort > 65535) {
            throw new IllegalArgumentException("本地SIP端口必须在1024-65535之间: " + sip.localPort);
        }

        if (sip.remotePort < 1024 || sip.remotePort > 65535) {
            throw new IllegalArgumentException("远程SIP端口必须在1024-65535之间: " + sip.remotePort);
        }
    }
}