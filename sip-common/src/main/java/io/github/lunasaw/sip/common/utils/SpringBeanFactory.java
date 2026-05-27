package io.github.lunasaw.sip.common.utils;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Bean工厂，提供静态方式获取Spring容器中已初始化的Bean。
 */
@Component
public class SpringBeanFactory implements ApplicationContextAware {

    // Spring应用上下文环境
    @Getter
    private static ApplicationContext applicationContext;

    /**
     * 实现ApplicationContextAware接口的回调方法，设置上下文环境
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        SpringBeanFactory.applicationContext = applicationContext;
    }

    /**
     * 根据 beanId 获取 Spring 容器中的 Bean。
     *
     * @param beanId Bean名称
     * @return Bean实例，容器未初始化时返回null
     */
    public static <T> T getBean(String beanId) throws BeansException {
        if (applicationContext == null) {
            return null;
        }
        return (T)applicationContext.getBean(beanId);
    }

    /**
     * 获取当前激活的 Spring Profile 名称。
     *
     * @return 当前激活的 Profile 名称
     */
    public static String getActiveProfile() {
        return applicationContext.getEnvironment().getActiveProfiles()[0];
    }

}
