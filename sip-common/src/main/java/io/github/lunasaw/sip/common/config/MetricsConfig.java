package io.github.lunasaw.sip.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控指标配置类
 * 当Spring容器中没有MeterRegistry Bean时，自动提供一个SimpleMeterRegistry
 *
 * @author luna
 * @date 2024/1/6
 */
@Configuration
public class MetricsConfig {

    /**
     * 提供MeterRegistry Bean
     * 当Spring容器中没有MeterRegistry Bean时，自动提供一个SimpleMeterRegistry
     * 这样可以确保SipMetrics类能够正常工作
     */
    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}