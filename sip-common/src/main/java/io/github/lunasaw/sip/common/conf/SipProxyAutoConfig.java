package io.github.lunasaw.sip.common.conf;

import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessor;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.transmit.event.response.*;
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
import java.util.List;
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
        // 获取响应处理器注册表
        ResponseProcessorRegistry registry = applicationContext.getBean(ResponseProcessorRegistry.class);

        // 注册所有响应处理策略
        registerResponseStrategies(registry);

        // 注册所有响应处理器
        registerResponseProcessors(registry);

        // 注册所有请求处理器
        registerRequestProcessors();

        log.info("SIP处理器注册完成 - {}", registry.getRegistryStats());
    }

    /**
     * 注册响应处理策略
     * 从Spring容器中获取所有ResponseProcessStrategy实现类并注册
     *
     * @param registry 响应处理器注册表
     */
    private void registerResponseStrategies(ResponseProcessorRegistry registry) {
        Map<String, ResponseProcessStrategy> strategyMap =
                applicationContext.getBeansOfType(ResponseProcessStrategy.class);

        strategyMap.forEach((beanName, strategy) -> {
            try {
                // 检查策略是否支持特定方法
                if (strategy.supportsMethod("*")) {
                    // 支持所有方法，注册到通用处理器
                    registry.registerStrategy("*", strategy);
                } else {
                    // 支持特定方法，需要从策略中获取支持的方法列表
                    // 这里可以通过注解或其他方式获取支持的方法
                    log.debug("发现响应处理策略: {} -> {}", beanName, strategy.getStrategyName());
                }
            } catch (Exception e) {
                log.error("注册响应处理策略失败: bean = {}", beanName, e);
            }
        });

        log.info("注册响应处理策略完成: {} 个策略", strategyMap.size());
    }

    /**
     * 注册响应处理器
     * 使用新的注册表机制，支持策略组合
     *
     * @param registry 响应处理器注册表
     */
    private void registerResponseProcessors(ResponseProcessorRegistry registry) {
        Map<String, SipResponseProcessor> processorMap =
            applicationContext.getBeansOfType(SipResponseProcessor.class);

        processorMap.forEach((beanName, processor) -> {
            try {
                if (processor instanceof AbstractSipResponseProcessor) {
                    AbstractSipResponseProcessor abstractProcessor = (AbstractSipResponseProcessor) processor;
                    String method = abstractProcessor.getMethod();

                    if (method != null) {
                        // 获取该方法的策略列表
                        List<ResponseProcessStrategy> strategies = registry.getStrategies(method);

                        if (!strategies.isEmpty()) {
                            // 创建组合处理器
                            CompositeResponseProcessor compositeProcessor =
                                    new CompositeResponseProcessor(method, strategies);

                            // 注册到SIP监听器
                            CustomerSipListener.getInstance().addResponseProcessor(method, compositeProcessor);

                            // 注册到注册表
                            registry.registerProcessor(method, compositeProcessor);

                            log.debug("注册组合响应处理器: {} -> {} (策略数量: {})",
                                    method, compositeProcessor.getClass().getSimpleName(), strategies.size());
                        } else {
                            // 没有策略，使用原始处理器
                            CustomerSipListener.getInstance().addResponseProcessor(method, processor);
                            registry.registerProcessor(method, processor);

                            log.debug("注册响应处理器: {} -> {}", method, processor.getClass().getSimpleName());
                        }
                    }
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
                    log.debug("注册请求处理器: {} -> {}", method, processor.getClass().getSimpleName());
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
