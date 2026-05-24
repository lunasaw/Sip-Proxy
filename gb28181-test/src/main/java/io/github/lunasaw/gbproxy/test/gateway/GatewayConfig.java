package io.github.lunasaw.gbproxy.test.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * sip-gateway 参考实现的装配。业务方可整体覆盖：
 * <ul>
 *   <li>替换 {@link InviteContextStore} → Redis 实现（多节点必须）</li>
 *   <li>替换 {@link BusinessNotifier} → 实际的 HTTP/MQ 推送</li>
 *   <li>替换 {@code nodeAddressMap} 装配方式 → K8s Endpoints / Nacos / Consul</li>
 * </ul>
 *
 * <p>所有 Bean 均使用 {@code @ConditionalOnMissingBean}，业务方覆盖时不需要排除。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    @ConditionalOnMissingBean
    public InviteContextStore inviteContextStore(GatewayProperties properties) {
        return new InMemoryInviteContextStore(properties.getInviteContextTtlMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessNotifier businessNotifier() {
        return new NoopBusinessNotifier();
    }

    /**
     * 仅当配置了多节点时装配 RestTemplate；单机部署省 RestTemplate 依赖。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "gateway", name = "nodes")
    public RestTemplate gatewayRestTemplate() {
        return new RestTemplate();
    }
}
