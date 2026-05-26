package io.github.lunasaw.sip.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SIP 协议通用配置属性类 - 支持外部化配置
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
     * UserAgent 头默认值，可通过 sip.common.user-agent 覆盖
     */
    private String userAgent = "sip-proxy";

    /**
     * 时间同步配置
     */
    private TimeSync timeSync = new TimeSync();

    /**
     * GBT-28181-2022 附录 I 协议版本标识 X-GB-Ver。
     * 默认 3.0（GBT-28181-2022），可通过 sip.common.protocol-version 覆盖。
     */
    private String protocolVersion = "3.0";

    /**
     * GBT-28181-2022 附录 J 目录类型语义版本：2016 / 2022。
     * 2016 标准下 215=虚拟组织，216=中心信令控制服务器；
     * 2022 标准下 215=业务分组，216=虚拟组织。
     * 默认 2022，可通过 sip.common.directory-version 覆盖。
     */
    private String directoryVersion = "2022";

    /**
     * §8.3 SIP 信令认证扩展配置（默认关闭，开启后注入 Note 头域）。
     */
    private SignalAuth signalAuth = new SignalAuth();


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

    /**
     * §8.3 SIP 信令认证扩展配置。
     *
     * <p>默认 {@link #enabled}=false，开启后框架会在 REGISTER 注入 Note 头域，
     * 并按 {@link #algorithm} 计算摘要。{@code algorithm} 取值 {@code MD5} / {@code SM3}。
     */
    @Data
    public static class SignalAuth {
        /** 是否启用 §8.3 信令认证扩展（注入 Note 头域）。 */
        private boolean enabled = false;

        /** 数字摘要算法：MD5（默认，RFC 3261）/ SM3（GBT-28181-2022 §8.3 推荐）。 */
        private String  algorithm = "MD5";

        /** 信令安全路由网关 ID（启用 Monitor-User-Identity 时必填，跨域转发链首段）。 */
        private String  gatewayId;

        /** 用户 ID（启用 Monitor-User-Identity 时必填，跨域转发链次段）。 */
        private String  userId;

        /** 用户身份属性（隶属机构/类别/职级），跨域转发链尾段，可选。 */
        private String  userAttribute;
    }
}