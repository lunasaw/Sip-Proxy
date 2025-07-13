package io.github.lunasaw.gbproxy.client.config;

import io.github.lunasaw.gbproxy.client.transmit.request.ack.AckRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.ack.DefaultAckRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.bye.DefaultByeProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.info.CustomInfoRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.info.InfoRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.invite.DefaultInviteRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.invite.InviteRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.CustomMessageRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.DefaultSubscribeProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.response.bye.ByeProcessorHandler;
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
@ComponentScan(basePackages = "io.github.lunasaw.gbproxy.client")
public class SipProxyClientAutoConfig implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() {
        Map<String, MessageClientHandlerAbstract> clientMessageHandlerMap = applicationContext.getBeansOfType(MessageClientHandlerAbstract.class);
        clientMessageHandlerMap.forEach((k, v) -> ClientMessageRequestProcessor.addHandler(v));

        Map<String, SubscribeHandlerAbstract> clientSubscribeHandlerMap =
                applicationContext.getBeansOfType(SubscribeHandlerAbstract.class);
        clientSubscribeHandlerMap.forEach((k, v) -> SubscribeRequestProcessor.addHandler(v));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageRequestHandler messageRequestHandler() {
        return new CustomMessageRequestHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ByeProcessorHandler byeProcessorHandler() {
        return new DefaultByeProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public InfoRequestHandler infoRequestHandler() {
        return new CustomInfoRequestHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscribeRequestHandler subscribeRequestHandler() {
        return new DefaultSubscribeProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AckRequestHandler ackRequestHandler() {
        return new DefaultAckRequestHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public InviteRequestHandler inviteRequestHandler() {
        return new DefaultInviteRequestHandler();
    }

}
