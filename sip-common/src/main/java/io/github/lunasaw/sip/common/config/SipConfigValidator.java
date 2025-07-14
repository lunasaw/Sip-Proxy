package io.github.lunasaw.sip.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.github.lunasaw.sip.common.async.SipAsyncConfig;
import io.github.lunasaw.sip.common.exception.SipConfigurationException;
import io.github.lunasaw.sip.common.pool.SipPoolConfig;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

/**
 * SIP配置验证器
 * 验证SIP相关配置参数的合理性和有效性
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
@Component
public class SipConfigValidator {

    private final SipAsyncConfig asyncConfig;
    private final SipPoolConfig  poolConfig;

    @Autowired
    public SipConfigValidator(SipAsyncConfig asyncConfig, SipPoolConfig poolConfig) {
        this.asyncConfig = asyncConfig;
        this.poolConfig = poolConfig;
    }

    /**
     * 应用启动时自动验证配置
     */
    @PostConstruct
    public void validateOnStartup() {
        validateAllConfigs();
    }

    /**
     * 验证所有SIP配置
     */
    public void validateAllConfigs() {
        log.info("开始验证SIP配置参数...");

        validateAsyncConfig();
        validatePoolConfig();
        validateCompatibility();

        log.info("SIP配置验证完成");
    }

    /**
     * 验证异步配置
     */
    public void validateAsyncConfig() {
        if (!asyncConfig.isEnabled()) {
            log.info("异步处理未启用，跳过异步配置验证");
            return;
        }

        // 验证线程池配置
        if (asyncConfig.getCorePoolSize() <= 0) {
            throw new SipConfigurationException("sip.async.corePoolSize",
                "核心线程数必须大于0，当前值: " + asyncConfig.getCorePoolSize());
        }

        if (asyncConfig.getMaxPoolSize() < asyncConfig.getCorePoolSize()) {
            throw new SipConfigurationException("sip.async.maxPoolSize",
                "最大线程数不能小于核心线程数，核心: " + asyncConfig.getCorePoolSize() +
                    ", 最大: " + asyncConfig.getMaxPoolSize());
        }

        if (asyncConfig.getQueueCapacity() <= 0) {
            throw new SipConfigurationException("sip.async.queueCapacity",
                "队列容量必须大于0，当前值: " + asyncConfig.getQueueCapacity());
        }

        if (asyncConfig.getKeepAliveSeconds() <= 0) {
            throw new SipConfigurationException("sip.async.keepAliveSeconds",
                "线程存活时间必须大于0，当前值: " + asyncConfig.getKeepAliveSeconds());
        }

        if (asyncConfig.getTimeoutMillis() <= 0) {
            throw new SipConfigurationException("sip.async.timeoutMillis",
                "异步处理超时时间必须大于0，当前值: " + asyncConfig.getTimeoutMillis());
        }

        if (!StringUtils.hasText(asyncConfig.getThreadNamePrefix())) {
            throw new SipConfigurationException("sip.async.threadNamePrefix",
                "线程名前缀不能为空");
        }

        log.debug("异步配置验证通过: 核心线程={}, 最大线程={}, 队列容量={}",
            asyncConfig.getCorePoolSize(), asyncConfig.getMaxPoolSize(), asyncConfig.getQueueCapacity());
    }

    /**
     * 验证连接池配置
     */
    public void validatePoolConfig() {
        if (!poolConfig.isEnabled()) {
            log.info("连接池未启用，跳过连接池配置验证");
            return;
        }

        // 验证连接数配置
        if (poolConfig.getMaxConnections() <= 0) {
            throw new SipConfigurationException("sip.pool.maxConnections",
                "最大连接数必须大于0，当前值: " + poolConfig.getMaxConnections());
        }

        if (poolConfig.getCoreConnections() < 0) {
            throw new SipConfigurationException("sip.pool.coreConnections",
                "核心连接数不能小于0，当前值: " + poolConfig.getCoreConnections());
        }

        if (poolConfig.getCoreConnections() > poolConfig.getMaxConnections()) {
            throw new SipConfigurationException("sip.pool.coreConnections",
                "核心连接数不能大于最大连接数，核心: " + poolConfig.getCoreConnections() +
                    ", 最大: " + poolConfig.getMaxConnections());
        }

        if (poolConfig.getMaxIdleConnections() <= 0) {
            throw new SipConfigurationException("sip.pool.maxIdleConnections",
                "最大空闲连接数必须大于0，当前值: " + poolConfig.getMaxIdleConnections());
        }

        // 验证超时配置
        if (poolConfig.getConnectionTimeoutMillis() <= 0) {
            throw new SipConfigurationException("sip.pool.connectionTimeoutMillis",
                "连接超时时间必须大于0，当前值: " + poolConfig.getConnectionTimeoutMillis());
        }

        if (poolConfig.getIdleTimeoutMillis() <= 0) {
            throw new SipConfigurationException("sip.pool.idleTimeoutMillis",
                "空闲超时时间必须大于0，当前值: " + poolConfig.getIdleTimeoutMillis());
        }

        if (poolConfig.getCleanupIntervalMillis() <= 0) {
            throw new SipConfigurationException("sip.pool.cleanupIntervalMillis",
                "清理间隔时间必须大于0，当前值: " + poolConfig.getCleanupIntervalMillis());
        }

        if (poolConfig.getMaxWaitMillis() <= 0) {
            throw new SipConfigurationException("sip.pool.maxWaitMillis",
                "最大等待时间必须大于0，当前值: " + poolConfig.getMaxWaitMillis());
        }

        // 验证验证查询配置
        if (!StringUtils.hasText(poolConfig.getValidationQuery())) {
            throw new SipConfigurationException("sip.pool.validationQuery",
                "验证查询不能为空");
        }

        log.debug("连接池配置验证通过: 最大连接={}, 核心连接={}, 最大空闲={}",
            poolConfig.getMaxConnections(), poolConfig.getCoreConnections(), poolConfig.getMaxIdleConnections());
    }

    /**
     * 验证配置兼容性
     */
    public void validateCompatibility() {
        // 检查异步和连接池的兼容性
        if (asyncConfig.isEnabled() && poolConfig.isEnabled()) {
            // 建议连接池最大连接数不少于异步线程池最大线程数
            if (poolConfig.getMaxConnections() < asyncConfig.getMaxPoolSize()) {
                log.warn("连接池最大连接数({})小于异步线程池最大线程数({})，可能导致性能瓶颈",
                    poolConfig.getMaxConnections(), asyncConfig.getMaxPoolSize());
            }

            // 检查超时时间的合理性
            if (poolConfig.getConnectionTimeoutMillis() > asyncConfig.getTimeoutMillis()) {
                log.warn("连接池连接超时时间({})大于异步处理超时时间({})，可能导致异步任务提前超时",
                    poolConfig.getConnectionTimeoutMillis(), asyncConfig.getTimeoutMillis());
            }
        }

        // 检查资源配置的合理性
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (asyncConfig.isEnabled() && asyncConfig.getMaxPoolSize() > availableProcessors * 4) {
            log.warn("异步线程池最大线程数({})过大，建议不超过CPU核心数的4倍({})",
                asyncConfig.getMaxPoolSize(), availableProcessors * 4);
        }

        log.debug("配置兼容性验证完成");
    }

    /**
     * 获取配置摘要
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("SIP配置摘要:\n");

        if (asyncConfig.isEnabled()) {
            summary.append(String.format("  异步处理: 启用 (核心线程=%d, 最大线程=%d, 队列=%d)\n",
                asyncConfig.getCorePoolSize(), asyncConfig.getMaxPoolSize(), asyncConfig.getQueueCapacity()));
        } else {
            summary.append("  异步处理: 禁用\n");
        }

        if (poolConfig.isEnabled()) {
            summary.append(String.format("  连接池: 启用 (最大连接=%d, 核心连接=%d, 最大空闲=%d)\n",
                poolConfig.getMaxConnections(), poolConfig.getCoreConnections(), poolConfig.getMaxIdleConnections()));
        } else {
            summary.append("  连接池: 禁用\n");
        }

        return summary.toString();
    }
}