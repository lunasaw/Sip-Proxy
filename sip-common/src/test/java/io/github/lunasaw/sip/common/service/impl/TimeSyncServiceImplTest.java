package io.github.lunasaw.sip.common.service.impl;

import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.service.TimeSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 时间同步服务测试类
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class TimeSyncServiceImplTest {

    @Mock
    private SipCommonProperties sipCommonProperties;

    @Mock
    private SipCommonProperties.TimeSync timeSyncConfig;

    private TimeSyncService timeSyncService;

    @BeforeEach
    void setUp() {
        timeSyncService = new TimeSyncServiceImpl();
        ReflectionTestUtils.setField(timeSyncService, "gb28181Properties", sipCommonProperties);
        
        // 设置默认的mock行为
        when(sipCommonProperties.getTimeSync()).thenReturn(timeSyncConfig);
        when(timeSyncConfig.isEnabled()).thenReturn(true);
        when(timeSyncConfig.getOffsetThreshold()).thenReturn(1000L);
    }

    @Test
    void testSyncTimeFromSip_Success() {
        // 准备测试数据
        LocalDateTime testTime = LocalDateTime.now();
        String dateValue = testTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        
        // 执行测试
        boolean result = timeSyncService.syncTimeFromSip(dateValue);
        
        // 验证结果
        assertTrue(result);
        assertNotNull(timeSyncService.getLastSyncTime());
    }

    @Test
    void testSyncTimeFromSip_DisabledConfig() {
        // 设置时间同步功能为禁用
        when(timeSyncConfig.isEnabled()).thenReturn(false);
        
        // 执行测试
        boolean result = timeSyncService.syncTimeFromSip("2024-01-01T12:00:00.000");
        
        // 验证结果
        assertFalse(result);
    }

    @Test
    void testSyncTimeFromSip_InvalidDateFormat() {
        // 使用无效的日期格式
        String invalidDate = "invalid-date-format";
        
        // 执行测试
        boolean result = timeSyncService.syncTimeFromSip(invalidDate);
        
        // 验证结果
        assertFalse(result);
    }

    @Test
    void testSyncTimeFromSip_NullOrEmpty() {
        // 测试null值
        boolean result1 = timeSyncService.syncTimeFromSip(null);
        assertFalse(result1);
        
        // 测试空字符串
        boolean result2 = timeSyncService.syncTimeFromSip("");
        assertFalse(result2);
        
        // 测试空白字符串
        boolean result3 = timeSyncService.syncTimeFromSip("   ");
        assertFalse(result3);
    }

    @Test
    void testSyncTimeFromNtp_DisabledConfig() {
        // 设置时间同步功能为禁用
        when(timeSyncConfig.isEnabled()).thenReturn(false);
        
        // 执行测试
        boolean result = timeSyncService.syncTimeFromNtp("pool.ntp.org");
        
        // 验证结果
        assertFalse(result);
    }

    @Test
    void testSyncTimeFromNtp_NullOrEmptyServer() {
        // 测试null值
        boolean result1 = timeSyncService.syncTimeFromNtp(null);
        assertFalse(result1);
        
        // 测试空字符串
        boolean result2 = timeSyncService.syncTimeFromNtp("");
        assertFalse(result2);
        
        // 测试空白字符串
        boolean result3 = timeSyncService.syncTimeFromNtp("   ");
        assertFalse(result3);
    }

    @Test
    void testTimeOffsetOperations() {
        // 测试设置时间偏差
        long testOffset = 500L;
        timeSyncService.setTimeOffset(testOffset);
        
        // 验证结果
        assertEquals(testOffset, timeSyncService.getTimeOffset());
        assertNotNull(timeSyncService.getLastSyncTime());
    }

    @Test
    void testNeedsTimeSync() {
        // 测试不需要校时的情况（偏差小于阈值）
        timeSyncService.setTimeOffset(500L); // 小于1000ms阈值
        assertFalse(timeSyncService.needsTimeSync());
        
        // 测试需要校时的情况（偏差超过阈值）
        timeSyncService.setTimeOffset(1500L); // 大于1000ms阈值
        assertTrue(timeSyncService.needsTimeSync());
        
        // 测试禁用时间同步的情况
        when(timeSyncConfig.isEnabled()).thenReturn(false);
        assertFalse(timeSyncService.needsTimeSync());
    }

    @Test
    void testGetCorrectedTime() {
        // 设置时间偏差
        long offset = 1000L; // 1秒
        timeSyncService.setTimeOffset(offset);
        
        // 获取修正后的时间
        LocalDateTime correctedTime = timeSyncService.getCorrectedTime();
        LocalDateTime currentTime = LocalDateTime.now();
        
        // 验证修正后的时间应该比当前时间早大约1秒
        assertNotNull(correctedTime);
        assertTrue(correctedTime.isBefore(currentTime));
    }

    @Test
    void testSipDateParsing() {
        // 测试各种SIP日期格式
        String[] validDateFormats = {
            "2024-01-01T12:00:00.000",
            "2024-12-31T23:59:59.999",
            "2024-06-15T08:30:45.123"
        };
        
        for (String dateFormat : validDateFormats) {
            boolean result = timeSyncService.syncTimeFromSip(dateFormat);
            assertTrue(result, "Failed to parse date format: " + dateFormat);
        }
    }

    @Test
    void testTimeSyncWithLargeOffset() {
        // 设置较大的偏差阈值
        when(timeSyncConfig.getOffsetThreshold()).thenReturn(100L);
        
        // 使用一个过去的时间来产生较大的偏差
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(5);
        String dateValue = pastTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        
        // 执行时间同步
        boolean result = timeSyncService.syncTimeFromSip(dateValue);
        
        // 验证结果
        assertTrue(result);
        assertTrue(timeSyncService.needsTimeSync()); // 应该需要校时
    }
}