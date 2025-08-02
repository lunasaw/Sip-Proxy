package io.github.lunasaw.gbproxy.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Voglander SIP服务端配置属性
 *
 * @author luna
 * @since 2025/8/2
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.server")
public class SipServerProperties {

    /**
     * 是否启用服务端
     */
    private boolean enabled = false;

    /**
     * 服务器IP
     */
    private String ip = "127.0.0.1";

    /**
     * 服务器端口
     */
    private int port = 5060;

    /**
     * 最大设备数
     */
    private int maxDevices = 100;

    /**
     * 设备超时时间
     */
    private String deviceTimeout = "5m";

    /**
     * 是否启用TCP
     */
    private boolean enableTcp = false;

    /**
     * 是否启用UDP
     */
    private boolean enableUdp = true;

    /**
     * 域
     */
    private String domain = "34020000002000000001";

    /**
     * 服务器ID
     */
    private String serverId = "34020000002000000001";

    /**
     * 服务器名称
     */
    private String serverName = "GB28181-Server";


    /**
     * 用户名
     */
    private String username = "admin";


    /**
     * 密码
     */
    private String password;


    /**
     * 域
     */
    private String realm = "34020000";
}