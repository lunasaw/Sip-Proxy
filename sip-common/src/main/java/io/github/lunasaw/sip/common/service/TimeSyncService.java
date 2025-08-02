package io.github.lunasaw.sip.common.service;

import java.time.LocalDateTime;

/**
 * 时间同步服务接口
 * 支持SIP和NTP两种校时方式
 *
 * @author luna
 */
public interface TimeSyncService {

    /**
     * SIP校时 - 从Date头域解析时间并同步
     *
     * @param dateHeaderValue Date头域的值 (格式: yyyy-MM-dd'T'HH:mm:ss.SSS)
     * @return 是否同步成功
     */
    boolean syncTimeFromSip(String dateHeaderValue);

    /**
     * NTP校时 - 从NTP服务器同步时间
     *
     * @param ntpServer NTP服务器地址
     * @return 是否同步成功
     */
    boolean syncTimeFromNtp(String ntpServer);

    /**
     * 获取当前系统与标准时间的偏差
     *
     * @return 时间偏差(毫秒)，正值表示本地时间快于标准时间
     */
    long getTimeOffset();

    /**
     * 设置时间偏差
     *
     * @param offset 时间偏差(毫秒)
     */
    void setTimeOffset(long offset);

    /**
     * 获取经过校时修正的当前时间
     *
     * @return 修正后的当前时间
     */
    LocalDateTime getCorrectedTime();

    /**
     * 检查是否需要校时
     * 当时间偏差超过配置的阈值时返回true
     *
     * @return 是否需要校时
     */
    boolean needsTimeSync();

    /**
     * 获取上次校时的时间
     *
     * @return 上次校时的时间
     */
    LocalDateTime getLastSyncTime();
}