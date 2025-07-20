package io.github.lunasaw.gbproxy.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务端测试配置类
 * 配置GB28181服务端测试相关参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "test.server")
public class TestServerConfig {

    /**
     * 服务端监听IP
     */
    private String ip = "127.0.0.1";

    /**
     * 服务端监听端口
     */
    private int port = 5060;

    /**
     * SIP域（国标编码）
     */
    private String domain = "44050100";

    /**
     * 服务端ID（20位国标编码）
     */
    private String serverId = "44050100002000000001";

    /**
     * 服务端名称
     */
    private String serverName = "GB28181-Server-Test";

    /**
     * 认证用户名
     */
    private String username = "admin";

    /**
     * 认证密码
     */
    private String password = "123456";

    /**
     * 认证域
     */
    private String realm = "44050100";

    /**
     * 设备注册超时时间（秒）
     */
    private int registerTimeout = 60;

    /**
     * 心跳超时时间（秒）
     */
    private int heartbeatTimeout = 30;

    /**
     * 邀请超时时间（秒）
     */
    private int inviteTimeout = 30;

    /**
     * 最大并发设备数
     */
    private int maxDevices = 1000;

    /**
     * 是否启用认证
     */
    private boolean authEnabled = true;

    /**
     * 是否启用设备状态监控
     */
    private boolean deviceMonitorEnabled = true;

    /**
     * 媒体服务器配置
     */
    private MediaServer mediaServer = new MediaServer();

    @Data
    public static class MediaServer {
        /**
         * 媒体服务器IP
         */
        private String ip = "127.0.0.1";

        /**
         * RTP端口范围起始
         */
        private int rtpPortStart = 10000;

        /**
         * RTP端口范围结束
         */
        private int rtpPortEnd = 20000;

        /**
         * RTCP端口范围起始
         */
        private int rtcpPortStart = 20001;

        /**
         * RTCP端口范围结束
         */
        private int rtcpPortEnd = 30000;
    }
}