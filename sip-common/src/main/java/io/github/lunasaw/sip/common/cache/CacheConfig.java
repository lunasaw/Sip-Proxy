package io.github.lunasaw.sip.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * 缓存配置类 - 使用Caffeine替代ConcurrentHashMap提升性能
 *
 * @author luna
 * @date 2024/1/6
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 默认缓存管理器 - 使用ConcurrentMapCacheManager作为后备
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("devices", "subscribes", "transactions", "sipMessages");
    }

    /**
     * Caffeine设备信息缓存
     */
    @Bean("deviceCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> deviceCache() {
        return Caffeine.newBuilder()
            .maximumSize(50000) // 支持更多设备
            .expireAfterWrite(Duration.ofHours(2)) // 写入后2小时过期
            .expireAfterAccess(Duration.ofMinutes(30)) // 访问后30分钟过期
            .recordStats()
            .build();
    }

    /**
     * Caffeine订阅信息缓存
     */
    @Bean("subscribeCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> subscribeCache() {
        return Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofMinutes(5)) // 订阅信息5分钟过期
            .expireAfterAccess(Duration.ofMinutes(2))
            .recordStats()
            .build();
    }

    /**
     * Caffeine事务缓存
     */
    @Bean("transactionCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> transactionCache() {
        return Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(1)) // 事务信息1分钟过期
            .recordStats()
            .build();
    }

    /**
     * Caffeine SIP消息缓存
     */
    @Bean("sipMessageCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> sipMessageCache() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(30)) // 写入后30分钟过期
            .expireAfterAccess(Duration.ofMinutes(15)) // 访问后15分钟过期
            .recordStats()
            .build();
    }
}