package io.github.lunasaw.gbproxy.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Voglander SIP客户端配置属性
 *
 * @author luna
 * @since 2025/8/2
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.client")
public class SipClientProperties {

    /**
     * 是否启用客户端
     */
    private boolean enabled = false;

    /**
     * 客户端ID
     */
    private String clientId = "34020000001320000001";

    /**
     * 客户端名称
     */
    private String clientName = "GB28181-Client";

    /**
     * 保活间隔
     */
    private String keepAliveInterval = "1m";

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试延迟
     */
    private String retryDelay = "5s";

    /**
     * 注册过期时间(秒)
     */
    private int registerExpires = 3600;

    /**
     * 客户端IP
     */
    private String domain = "127.0.0.1";

    /**
     * 客户端端口
     */
    private int port = 5061;
}