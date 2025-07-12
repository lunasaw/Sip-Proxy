package io.github.lunasaw.gbproxy.client.config;

import io.github.lunasaw.gbproxy.client.transmit.request.ack.AckRequestProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.ack.CustomAckRequestProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.bye.ByeProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.bye.CustomByeProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.info.CustomInfoProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.info.InfoProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.invite.CustomInviteProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.invite.InviteProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.CustomMessageProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.ClientSubscribeRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.CustomSubscribeProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeProcessorClient;
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

        Map<String, SubscribeClientHandlerAbstract> clientSubscribeHandlerMap =
                applicationContext.getBeansOfType(SubscribeClientHandlerAbstract.class);
        clientSubscribeHandlerMap.forEach((k, v) -> ClientSubscribeRequestProcessor.addHandler(v));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageProcessorClient messageProcessorClient() {
        return new CustomMessageProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public ByeProcessorClient byeProcessorClient() {
        return new CustomByeProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public InfoProcessorClient infoProcessorClient() {
        return new CustomInfoProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscribeProcessorClient inviteResponseProcessorClient() {
        return new CustomSubscribeProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public AckRequestProcessorClient ackRequestProcessorClient() {
        return new CustomAckRequestProcessorClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public InviteProcessorClient inviteProcessorClient() {
        return new CustomInviteProcessorClient();
    }

}
