package io.github.lunasaw.sip.common.service.impl;

import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.service.TimeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 时间同步服务实现类
 * 支持SIP和NTP两种校时方式
 *
 * @author luna
 */
@Slf4j
@Service
public class TimeSyncServiceImpl implements TimeSyncService {

    private static final DateTimeFormatter SIP_DATE_FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter SIP_DATE_FORMATTER_WITHOUT_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    // NTP时间戳偏移量 (1900年1月1日到1970年1月1日的秒数)
    private static final long NTP_TIMESTAMP_OFFSET = 2208988800L;
    
    // 时间偏差(毫秒)
    private final AtomicLong timeOffset = new AtomicLong(0);
    
    // 上次校时时间
    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();

    @Autowired
    private SipCommonProperties sipCommonProperties;

    /**
     * 解析SIP Date头域时间，支持有无毫秒两种格式
     *
     * @param dateTime 时间字符串
     * @return 解析后的LocalDateTime
     * @throws DateTimeParseException 解析失败时抛出异常
     */
    private LocalDateTime parseSipDateTime(String dateTime) throws DateTimeParseException {
        try {
            // 首先尝试解析带毫秒的格式
            return LocalDateTime.parse(dateTime, SIP_DATE_FORMATTER_WITH_MILLIS);
        } catch (DateTimeParseException e) {
            // 如果失败，尝试解析不带毫秒的格式
            return LocalDateTime.parse(dateTime, SIP_DATE_FORMATTER_WITHOUT_MILLIS);
        }
    }

    @Override
    public boolean syncTimeFromSip(String dateHeaderValue) {
        SipCommonProperties.TimeSync timeSyncConfig = sipCommonProperties.getTimeSync();
        if (!timeSyncConfig.isEnabled()) {
            log.debug("时间同步功能已禁用");
            return false;
        }

        if (dateHeaderValue == null || dateHeaderValue.trim().isEmpty()) {
            log.warn("SIP Date头域值为空，无法进行时间同步");
            return false;
        }

        try {
            // 解析SIP Date头域的时间，支持有无毫秒两种格式
            LocalDateTime serverTime = parseSipDateTime(dateHeaderValue.trim());
            LocalDateTime localTime = LocalDateTime.now();
            
            // 计算时间偏差
            long offset = java.time.Duration.between(serverTime, localTime).toMillis();
            
            log.info("SIP校时 - 服务器时间: {}, 本地时间: {}, 偏差: {}ms", 
                    serverTime, localTime, offset);
            
            // 更新时间偏差
            timeOffset.set(offset);
            lastSyncTime.set(LocalDateTime.now());
            
            long offsetThreshold = timeSyncConfig.getOffsetThreshold();
            if (Math.abs(offset) > offsetThreshold) {
                log.warn("时间偏差较大: {}ms，超过阈值: {}ms", offset, offsetThreshold);
            } else {
                log.info("SIP时间同步成功，偏差: {}ms", offset);
            }
            
            return true;
        } catch (DateTimeParseException e) {
            log.error("解析SIP Date头域失败: {}", dateHeaderValue, e);
            return false;
        } catch (Exception e) {
            log.error("SIP时间同步异常", e);
            return false;
        }
    }

    @Override
    public boolean syncTimeFromNtp(String ntpServer) {
        SipCommonProperties.TimeSync timeSyncConfig = sipCommonProperties.getTimeSync();
        if (!timeSyncConfig.isEnabled()) {
            log.debug("时间同步功能已禁用");
            return false;
        }

        if (ntpServer == null || ntpServer.trim().isEmpty()) {
            log.warn("NTP服务器地址为空，无法进行时间同步");
            return false;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000); // 5秒超时
            
            InetAddress address = InetAddress.getByName(ntpServer);
            
            // 构建NTP请求包
            byte[] requestData = new byte[48];
            requestData[0] = 0x1B; // LI=0, VN=3, Mode=3 (client)
            
            DatagramPacket requestPacket = new DatagramPacket(
                    requestData, requestData.length, address, 123);
            
            long localTimeBefore = System.currentTimeMillis();
            socket.send(requestPacket);
            
            // 接收响应
            byte[] responseData = new byte[48];
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
            socket.receive(responsePacket);
            long localTimeAfter = System.currentTimeMillis();
            
            // 解析NTP时间戳 (从字节32-35，网络字节序)
            long ntpTime = 0;
            for (int i = 40; i <= 43; i++) {
                ntpTime = (ntpTime << 8) | (responseData[i] & 0xff);
            }
            
            // 转换为Unix时间戳
            long serverTime = (ntpTime - NTP_TIMESTAMP_OFFSET) * 1000;
            long localTime = (localTimeBefore + localTimeAfter) / 2;
            
            long offset = localTime - serverTime;
            
            log.info("NTP校时 - 服务器时间: {}, 本地时间: {}, 偏差: {}ms", 
                    new java.util.Date(serverTime), new java.util.Date(localTime), offset);
            
            // 更新时间偏差
            timeOffset.set(offset);
            lastSyncTime.set(LocalDateTime.now());
            
            long offsetThreshold = timeSyncConfig.getOffsetThreshold();
            if (Math.abs(offset) > offsetThreshold) {
                log.warn("时间偏差较大: {}ms，超过阈值: {}ms", offset, offsetThreshold);
            } else {
                log.info("NTP时间同步成功，偏差: {}ms", offset);
            }
            
            return true;
        } catch (Exception e) {
            log.error("NTP时间同步失败: {}", ntpServer, e);
            return false;
        }
    }

    @Override
    public long getTimeOffset() {
        return timeOffset.get();
    }

    @Override
    public void setTimeOffset(long offset) {
        timeOffset.set(offset);
        lastSyncTime.set(LocalDateTime.now());
        log.info("手动设置时间偏差: {}ms", offset);
    }

    @Override
    public LocalDateTime getCorrectedTime() {
        long offset = timeOffset.get();
        return LocalDateTime.now().minusNanos(offset * 1_000_000);
    }

    @Override
    public boolean needsTimeSync() {
        SipCommonProperties.TimeSync timeSyncConfig = sipCommonProperties.getTimeSync();
        if (!timeSyncConfig.isEnabled()) {
            return false;
        }
        
        long offset = Math.abs(timeOffset.get());
        return offset > timeSyncConfig.getOffsetThreshold();
    }

    @Override
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime.get();
    }
}