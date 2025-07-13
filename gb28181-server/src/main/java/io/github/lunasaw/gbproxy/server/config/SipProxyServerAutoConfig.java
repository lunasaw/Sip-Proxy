package io.github.lunasaw.gbproxy.server.config;

import io.github.lunasaw.gbproxy.server.transimit.request.bye.DefaultServerByeProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.bye.ServerByeProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.info.DefaultServerInfoProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.info.ServerInfoProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.message.DefaultServerMessageProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageRequestProcessor;
import io.github.lunasaw.gbproxy.server.transimit.request.notify.DefaultServerNotifyProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.notify.ServerNotifyProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.register.DefaultServerRegisterProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.register.ServerRegisterProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.response.invite.DefaultInviteResponseProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.response.invite.InviteResponseProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.response.subscribe.DefaultSubscribeResponseProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.response.subscribe.SubscribeResponseProcessorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author luna
 * @date 2023/10/16
 */
@Slf4j
@Component
@ComponentScan(basePackages = "io.github.lunasaw.gbproxy.server")
public class SipProxyServerAutoConfig implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() {
        Map<String, MessageServerHandlerAbstract> messageHandlerMap = applicationContext.getBeansOfType(MessageServerHandlerAbstract.class);
        messageHandlerMap.forEach((k, v) -> ServerMessageRequestProcessor.addHandler(v));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerMessageProcessorHandler messageProcessorServer() {
        return new DefaultServerMessageProcessorHandler();
    }


    @Bean
    @ConditionalOnMissingBean
    public ServerByeProcessorHandler byeProcessorServer() {
        return new DefaultServerByeProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerInfoProcessorHandler infoProcessorServer() {
        return new DefaultServerInfoProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerNotifyProcessorHandler notifyProcessorServer() {
        return new DefaultServerNotifyProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerRegisterProcessorHandler registerProcessorServer() {
        return new DefaultServerRegisterProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public InviteResponseProcessorHandler inviteResponseProcessorHandler() {
        return new DefaultInviteResponseProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscribeResponseProcessorHandler subscribeResponseProcessorHandler() {
        return new DefaultSubscribeResponseProcessorHandler();
    }

    // ==================== Request Handler Bean配置 ====================

    @Bean
    @ConditionalOnMissingBean
    public ServerRegisterProcessorHandler serverRegisterProcessorHandler() {
        return new DefaultServerRegisterProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerInfoProcessorHandler serverInfoProcessorHandler() {
        return new DefaultServerInfoProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerMessageProcessorHandler serverMessageProcessorHandler() {
        return new DefaultServerMessageProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerNotifyProcessorHandler serverNotifyProcessorHandler() {
        return new DefaultServerNotifyProcessorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerByeProcessorHandler serverByeProcessorHandler() {
        return new DefaultServerByeProcessorHandler();
    }

}
