package io.github.lunasaw.gbproxy.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端测试配置类
 * 配置GB28181客户端测试相关参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "test.client")
public class TestClientConfig {

    /**
     * 客户端IP
     */
    private String ip = "127.0.0.1";

    /**
     * 客户端端口
     */
    private int port = 5061;

    /**
     * 服务端IP
     */
    private String serverIp = "127.0.0.1";

    /**
     * 服务端端口
     */
    private int serverPort = 5060;

    /**
     * SIP域（国标编码）
     */
    private String domain = "44050100";

    /**
     * 设备ID（20位国标编码）
     */
    private String deviceId = "44050100001327000001";

    /**
     * 设备名称
     */
    private String deviceName = "GB28181-Client-Test";

    /**
     * 设备厂商
     */
    private String manufacturer = "TestManufacturer";

    /**
     * 设备型号
     */
    private String model = "TestModel";

    /**
     * 固件版本
     */
    private String firmware = "V1.0.0";

    /**
     * 认证用户名
     */
    private String username = "admin";

    /**
     * 认证密码
     */
    private String password = "123456";

    /**
     * 注册有效期（秒）
     */
    private int registerExpires = 3600;

    /**
     * 心跳间隔（秒）
     */
    private int keepaliveInterval = 60;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private long retryDelay = 5000;

    /**
     * 是否自动注册
     */
    private boolean autoRegister = true;

    /**
     * 是否自动心跳
     */
    private boolean autoKeepalive = true;

    /**
     * 设备通道配置
     */
    private List<DeviceChannel> channels;

    @Data
    public static class DeviceChannel {
        /**
         * 通道ID
         */
        private String channelId;

        /**
         * 通道名称
         */
        private String channelName;

        /**
         * 通道类型（1-球机，2-半球，3-固定枪机，4-遥控枪机）
         */
        private int channelType = 1;

        /**
         * 通道状态（ON-在线，OFF-离线）
         */
        private String status = "ON";

        /**
         * 父设备ID
         */
        private String parentId;

        /**
         * 安装地址
         */
        private String address;

        /**
         * 是否有录像
         */
        private boolean hasRecord = true;

        /**
         * 录像类型（time-定时录像，alarm-告警录像，manual-手动录像）
         */
        private String recordType = "time";
    }
}