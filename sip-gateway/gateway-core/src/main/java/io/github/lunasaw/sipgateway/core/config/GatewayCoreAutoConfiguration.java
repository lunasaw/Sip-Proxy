package io.github.lunasaw.sipgateway.core.config;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.sipgateway.core.notifier.NoopBusinessNotifier;
import io.github.lunasaw.sipgateway.core.web.GatewayDispatchController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * Gateway Core 自动装配。
 *
 * @author luna
 */
@AutoConfiguration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BusinessNotifier businessNotifier() {
        return new NoopBusinessNotifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandHandlerRegistry commandHandlerRegistry(ApplicationContext ctx,
                                                         GatewayProperties props) {
        return new CommandHandlerRegistry(ctx, props, ctx.getBeansOfType(io.github.lunasaw.sipgateway.core.api.ProtocolModule.class).values().stream().toList());
    }

    /**
     * Web 子配置：仅 servlet 环境启用。
     */
    @AutoConfiguration(after = GatewayCoreAutoConfiguration.class)
    @ConditionalOnWebApplication(type = SERVLET)
    @ConditionalOnClass(RestController.class)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public GatewayDispatchController gatewayDispatchController(
                GatewayProperties props,
                CommandHandlerRegistry registry) {
            return new GatewayDispatchController(props, registry);
        }

        @Bean("gatewayForwardRestTemplate")
        @ConditionalOnMissingBean(name = "gatewayForwardRestTemplate")
        public RestTemplate gatewayForwardRestTemplate() {
            return new RestTemplate();
        }
    }
}
