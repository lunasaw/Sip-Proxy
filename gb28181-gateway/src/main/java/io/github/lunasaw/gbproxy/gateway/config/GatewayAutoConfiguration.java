package io.github.lunasaw.gbproxy.gateway.config;

import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.forwarder.SipEventForwarder;
import io.github.lunasaw.gbproxy.gateway.notifier.NoopBusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.store.InMemoryInviteContextStore;
import io.github.lunasaw.gbproxy.gateway.web.SipCommandController;
import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * gb28181-gateway Spring Boot 自动装配。
 *
 * <p>所有 Bean 均使用 {@code @ConditionalOnMissingBean}，业务方可按需覆盖：
 * <ul>
 *   <li>替换 {@link InviteContextStore} → Redis 实现（多节点必须）</li>
 *   <li>替换 {@link BusinessNotifier} → 实际的 HTTP/MQ 推送</li>
 * </ul>
 *
 * @author luna
 */
@AutoConfiguration
@EnableConfigurationProperties(GatewayProperties.class)
@ConditionalOnClass(ServerCommandSender.class)
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InviteContextStore inviteContextStore(GatewayProperties props) {
        return new InMemoryInviteContextStore(props.getInviteContextTtlMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessNotifier businessNotifier() {
        return new NoopBusinessNotifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public SipEventForwarder sipEventForwarder(GatewayProperties props,
                                               InviteContextStore store,
                                               BusinessNotifier notifier,
                                               ObjectProvider<DeviceSessionCache> sessionCache) {
        return new SipEventForwarder(props, store, sessionCache.getIfAvailable(), notifier);
    }

    @AutoConfiguration(after = GatewayAutoConfiguration.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(RestController.class)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean(name = "gatewayForwardRestTemplate")
        public RestTemplate gatewayForwardRestTemplate() {
            return new RestTemplate();
        }

        @Bean
        @ConditionalOnMissingBean
        public SipCommandController sipCommandController(
                GatewayProperties props,
                ServerCommandSender sender,
                InviteContextStore store,
                @Qualifier("gatewayForwardRestTemplate") RestTemplate restTemplate) {
            return new SipCommandController(props, sender, store, restTemplate);
        }
    }
}
