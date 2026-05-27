package io.github.lunasaw.sip.common.conf;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * SIP协议栈默认配置工厂，从classpath加载基础配置并按需开启日志。
 */
@Slf4j
public class DefaultProperties {

    /**
     * 获取SIP协议栈配置属性。
     *
     * @param name   协议栈名称
     * @param ip     监听IP地址
     * @param sipLog 是否开启SIP日志
     * @return 配置属性对象
     */
    public static Properties getProperties(String name, String ip, boolean sipLog) {
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", name);
        properties.setProperty("javax.sip.IP_ADDRESS", ip);


        /**
         * sip_server_log.log 和 sip_debug_log.log ERROR, INFO, WARNING, OFF, DEBUG, TRACE
         */
        try {
            Resource resource = new ClassPathResource("sip/config.properties");
            InputStream inputStream = resource.getInputStream();
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (sipLog) {
            properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", "io.github.lunasaw.sip.common.conf.StackLoggerImpl");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOGGER", "io.github.lunasaw.sip.common.conf.ServerLoggerImpl");
            properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");
            log.info("[SIP日志]已开启");
        } else {
            log.info("[SIP日志]已关闭");
        }
        return properties;
    }
}
