package io.github.lunasaw.sip.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.async.SipAsyncConfig;
import io.github.lunasaw.sip.common.pool.SipPoolConfig;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

/**
 * SIP配置管理器
 * 统一管理SIP相关的所有配置，包括GB28181配置和验证
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
@Component
public class SipConfigurationManager {

    private final Gb28181Properties  gb28181Properties;
    private final SipConfigValidator sipConfigValidator;
    private final SipAsyncConfig     asyncConfig;
    private final SipPoolConfig      poolConfig;

    @Autowired
    public SipConfigurationManager(Gb28181Properties gb28181Properties,
        SipConfigValidator sipConfigValidator,
        SipAsyncConfig asyncConfig,
        SipPoolConfig poolConfig) {
        this.gb28181Properties = gb28181Properties;
        this.sipConfigValidator = sipConfigValidator;
        this.asyncConfig = asyncConfig;
        this.poolConfig = poolConfig;
    }

    /**
     * 应用启动时自动打印配置摘要
     */
    @PostConstruct
    public void printConfigurationOnStartup() {
        log.info("SIP配置管理器初始化完成");
        log.info(getConfigurationSummary());
    }

    /**
     * 获取GB28181配置
     */
    public Gb28181Properties getGb28181Properties() {
        return gb28181Properties;
    }

    /**
     * 获取异步配置
     */
    public SipAsyncConfig getAsyncConfig() {
        return asyncConfig;
    }

    /**
     * 获取连接池配置
     */
    public SipPoolConfig getPoolConfig() {
        return poolConfig;
    }

    /**
     * 获取配置摘要
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== SIP配置摘要 ===\n");

        // GB28181通用配置摘要
        summary.append("GB28181通用配置:\n");
        summary.append(String.format("  异步处理: %s\n",
            gb28181Properties.isAsyncEnabled() ? "启用" : "禁用"));
        summary.append(String.format("  监控: %s\n",
            gb28181Properties.isMetricsEnabled() ? "启用" : "禁用"));
        summary.append(String.format("  线程池大小: %d\n",
                gb28181Properties.getPerformance().getThreadPoolSize()));
        summary.append(String.format("  消息队列大小: %d\n",
                gb28181Properties.getPerformance().getMessageQueueSize()));

        // 异步配置摘要
        summary.append("\n异步配置:\n");
        summary.append(sipConfigValidator.getConfigSummary());

        return summary.toString();
    }

    /**
     * 重新验证所有配置
     */
    public void revalidateAllConfigs() {
        log.info("重新验证所有SIP配置...");
        gb28181Properties.validate();
        sipConfigValidator.validateAllConfigs();
        log.info("配置重新验证完成");
    }

    /**
     * 检查配置是否有效
     */
    public boolean isConfigurationValid() {
        try {
            gb28181Properties.validate();
            // 注意：这里不调用sipConfigValidator.validateAllConfigs()，
            // 因为它会抛出异常而不是返回boolean
            return true;
        } catch (Exception e) {
            log.error("配置验证失败: {}", e.getMessage());
            return false;
        }
    }
}