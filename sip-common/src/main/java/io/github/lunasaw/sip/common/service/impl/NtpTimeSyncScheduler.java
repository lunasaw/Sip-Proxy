package io.github.lunasaw.sip.common.service.impl;

import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.service.TimeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NTP定时校时任务
 * 根据配置定期执行NTP时间同步
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sip.common.time-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NtpTimeSyncScheduler {

    @Autowired
    private TimeSyncService timeSyncService;

    @Autowired
    private SipCommonProperties sipCommonProperties;

    /**
     * 定时执行NTP校时
     * 根据配置的同步间隔执行
     */
    @Scheduled(fixedRateString = "#{${sip.common.time-sync.ntp-sync-interval:3600} * 1000}")
    public void performNtpSync() {
        SipCommonProperties.TimeSync timeSyncConfig = sipCommonProperties.getTimeSync();
        
        // 检查是否启用NTP校时
        if (!timeSyncConfig.isEnabled()) {
            return;
        }

        SipCommonProperties.TimeSyncMode mode = timeSyncConfig.getMode();
        if (mode != SipCommonProperties.TimeSyncMode.NTP &&
                mode != SipCommonProperties.TimeSyncMode.BOTH) {
            return;
        }

        String ntpServer = timeSyncConfig.getNtpServer();
        if (ntpServer == null || ntpServer.trim().isEmpty()) {
            log.warn("NTP服务器地址未配置，跳过NTP校时");
            return;
        }

        log.debug("开始执行定时NTP校时，服务器: {}", ntpServer);
        
        try {
            boolean success = timeSyncService.syncTimeFromNtp(ntpServer);
            if (success) {
                log.info("定时NTP校时成功");
            } else {
                log.warn("定时NTP校时失败，服务器: {}", ntpServer);
            }
        } catch (Exception e) {
            log.error("定时NTP校时异常，服务器: {}", ntpServer, e);
        }
    }

    /**
     * 检查时间同步状态
     * 每5分钟检查一次时间偏差状态
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkTimeSyncStatus() {
        SipCommonProperties.TimeSync timeSyncConfig = sipCommonProperties.getTimeSync();
        
        if (!timeSyncConfig.isEnabled()) {
            return;
        }

        try {
            if (timeSyncService.needsTimeSync()) {
                long offset = timeSyncService.getTimeOffset();
                log.warn("时间偏差较大: {}ms，超过阈值: {}ms", offset, timeSyncConfig.getOffsetThreshold());
                
                // 如果配置了NTP作为备用校时方式，尝试NTP校时
                if (timeSyncConfig.getMode() == SipCommonProperties.TimeSyncMode.BOTH) {
                    log.info("尝试使用NTP进行时间校正");
                    timeSyncService.syncTimeFromNtp(timeSyncConfig.getNtpServer());
                }
            }
        } catch (Exception e) {
            log.error("检查时间同步状态异常", e);
        }
    }
}