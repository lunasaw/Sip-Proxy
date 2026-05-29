package io.github.lunasaw.sipgateway.gb28181.config;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.config.GatewayCoreAutoConfiguration;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.gb28181.forwarder.Gb28181EventForwarder;
import io.github.lunasaw.sipgateway.gb28181.handler.Gb28181Module;
import io.github.lunasaw.sipgateway.gb28181.handler.Gb28181WhitelistHandlers;
import io.github.lunasaw.sipgateway.gb28181.store.InMemoryInviteContextStore;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore;
import io.github.lunasaw.sipgateway.gb28181.web.Gb28181InviteResponseController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * GB28181 网关自动装配。
 *
 * <p>双重守门：
 * <ol>
 *   <li>{@code @ConditionalOnClass(ServerCommandSender)} — classpath 不含 gb28181-server 时整个模块跳过</li>
 *   <li>{@code @ConditionalOnBean(ServerCommandSender)} — 类存在但业务方未启用 @EnableSipServer 时跳过</li>
 *   <li>{@code @AutoConfiguration(after = GatewayCoreAutoConfiguration)} — 保证 CommandHandlerRegistry 装配时 Gb28181Module 已就位</li>
 * </ol>
 *
 * @author luna
 */
@AutoConfiguration(after = GatewayCoreAutoConfiguration.class)
@EnableConfigurationProperties(Gb28181GatewayProperties.class)
@ConditionalOnClass(ServerCommandSender.class)
@ConditionalOnBean(ServerCommandSender.class)
public class Gb28181GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InviteContextStore inviteContextStore(Gb28181GatewayProperties props) {
        return new InMemoryInviteContextStore(props.getInviteContextTtlMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public Gb28181Module gb28181Module() {
        return new Gb28181Module();
    }

    @Bean
    @ConditionalOnMissingBean
    public Gb28181WhitelistHandlers gb28181WhitelistHandlers(ServerCommandSender sender) {
        return new Gb28181WhitelistHandlers(sender);
    }

    @Bean
    @ConditionalOnMissingBean
    public Gb28181EventForwarder gb28181EventForwarder(BusinessNotifier notifier,
                                                       InviteContextStore store,
                                                       GatewayProperties coreProps,
                                                       Gb28181GatewayProperties gb28181Props) {
        return new Gb28181EventForwarder(notifier, store, coreProps, gb28181Props);
    }

    /**
     * Web 子配置：仅 servlet 环境启用。
     */
    @AutoConfiguration(after = Gb28181GatewayAutoConfiguration.class)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public Gb28181InviteResponseController gb28181InviteResponseController(
                GatewayProperties coreProps,
                ServerCommandSender sender,
                InviteContextStore store,
                @Qualifier("gatewayForwardRestTemplate") RestTemplate forward) {
            return new Gb28181InviteResponseController(coreProps, sender, store, forward);
        }
    }
}
