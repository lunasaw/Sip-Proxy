package io.github.lunasaw.sip.common.conf;

import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessor;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.transmit.event.response.SipResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sip.SipListener;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * SIP代理自动配置类
 * 使用新的注册表机制管理响应处理器，实现框架和业务分离
 *
 * @author luna
 */
@Slf4j
@ComponentScan("io.github.lunasaw.sip.common")
@Configuration
public class SipProxyAutoConfig implements InitializingBean, ApplicationContextAware {

    private static final String METHOD = "method";

    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    public SipListener sipListener() {
        // 默认使用同步监听器，可以通过配置切换为异步监听器
        return CustomerSipListener.getInstance();
    }

    @Override
    public void afterPropertiesSet() {
        // 注册所有响应处理器
        registerResponseProcessors();

        // 注册所有请求处理器
        registerRequestProcessors();
    }

    /**
     * 注册响应处理器
     * 使用新的注册表机制，支持策略组合
     */
    private void registerResponseProcessors() {
        Map<String, SipResponseProcessor> processorMap =
                applicationContext.getBeansOfType(SipResponseProcessor.class);

        processorMap.forEach((beanName, processor) -> {
            try {
                if (processor instanceof AbstractSipResponseProcessor) {
                    AbstractSipResponseProcessor abstractProcessor = (AbstractSipResponseProcessor) processor;
                    String method = abstractProcessor.getMethod();
                    CustomerSipListener.getInstance().addResponseProcessor(method, abstractProcessor);
                    log.info("注册响应处理器: {} -> {}", method, processor.getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.error("注册响应处理器失败: bean = {}", beanName, e);
            }
        });

        log.info("注册响应处理器完成: {} 个处理器", processorMap.size());
    }

    /**
     * 注册请求处理器
     * 保持原有的请求处理器注册逻辑
     */
    private void registerRequestProcessors() {
        Map<String, SipRequestProcessor> requestProcessorMap =
                applicationContext.getBeansOfType(SipRequestProcessor.class);

        requestProcessorMap.forEach((beanName, processor) -> {
            try {
                if (processor instanceof SipRequestProcessorAbstract) {
                    Field field = processor.getClass().getDeclaredField(METHOD);
                    field.setAccessible(true);
                    String method = field.get(processor).toString();
                    CustomerSipListener.getInstance().addRequestProcessor(method, processor);
                    log.info("注册请求处理器: {} -> {}", method, processor.getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.error("注册请求处理器失败: bean = {}", beanName, e);
            }
        });

        log.info("注册请求处理器完成: {} 个处理器", requestProcessorMap.size());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
