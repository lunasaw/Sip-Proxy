package io.github.lunasaw.sip.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GB28181通用配置属性类 - 支持外部化配置
 * 包含通用的性能配置和缓存配置，client和server特定配置已拆分到各自模块
 *
 * @author luna
 * @date 2024/1/6
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.common")
public class SipCommonProperties {

    /**
     * 时间同步配置
     */
    private TimeSync timeSync = new TimeSync();


    /**
     * 时间同步方式枚举
     */
    public enum TimeSyncMode {
        /**
         * 仅使用SIP校时
         */
        SIP,
        /**
         * 仅使用NTP校时
         */
        NTP,
        /**
         * 同时使用SIP和NTP校时
         */
        BOTH
    }

    /**
     * 时间同步配置
     */
    @Data
    public static class TimeSync {
        /**
         * 是否启用时间同步
         */
        private boolean enabled = true;

        /**
         * 时间同步方式: SIP, NTP, BOTH
         */
        private TimeSyncMode mode = TimeSyncMode.SIP;

        /**
         * 时间偏差阈值（毫秒）
         * 当时间偏差超过此值时会记录警告
         */
        private long offsetThreshold = 1000;

        /**
         * NTP服务器地址
         */
        private String ntpServer = "pool.ntp.org";

        /**
         * NTP校时间隔（秒）
         */
        private long ntpSyncInterval = 3600;

        /**
         * SIP注册过期时间建议值（秒）
         * 当时间偏差超过阈值时的注册过期时间
         */
        private int registerExpireOnError = 36000;

        /**
         * 是否在时间偏差较大时自动调整注册过期时间
         */
        private boolean autoAdjustExpire = true;
    }
}