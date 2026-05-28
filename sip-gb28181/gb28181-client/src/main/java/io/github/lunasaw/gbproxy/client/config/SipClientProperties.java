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

    /**
     * 对外可达IP（NAT/端口映射场景），填入 Via/Contact 头。
     * 不配置时 fallback 到 domain。
     */
    private String externalIp;

    /**
     * 对外可达端口（NAT/端口映射场景），填入 Via/Contact 头。
     * 不配置时 fallback 到 port。
     */
    private int externalPort = 0;

    /**
     * 获取对外可达 IP，优先返回 externalIp，未配置时返回 domain。
     *
     * @return 对外可达 IP 地址
     */
    public String getEffectiveIp() {
        return externalIp != null && !externalIp.isBlank() ? externalIp : domain;
    }

    /**
     * 获取对外可达端口，优先返回 externalPort，未配置时返回 port。
     *
     * @return 对外可达端口
     */
    public int getEffectivePort() {
        return externalPort > 0 ? externalPort : port;
    }
}