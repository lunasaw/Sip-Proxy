package io.github.lunasaw.gbproxy.test.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 测试专用配置类
 * 用于简化测试环境，禁用复杂的组件
 *
 * @author luna
 * @date 2025/01/23
 */
@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    // 排除可能导致问题的自动配置
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
})
public class TestConfiguration {

    // 测试环境专用配置
    // 这里可以添加测试专用的Bean配置
}