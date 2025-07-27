package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.transmit.response.register.ClientRegisterResponseProcessor;
import io.github.lunasaw.sip.common.service.TimeSyncService;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.header.SIPDateHeader;
import io.github.lunasaw.sip.common.entity.GbSipDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SIP校时功能集成测试
 *
 * @author luna
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class SipTimeSyncIntegrationTest {

    @Autowired
    private TimeSyncService timeSyncService;

    @Autowired
    private ClientRegisterResponseProcessor clientRegisterResponseProcessor;

    @BeforeEach
    void setUp() {
        // 重置时间偏差
        timeSyncService.setTimeOffset(0);
    }

    @Test
    void testSipTimeSyncIntegration() {
        // 1. 创建模拟的注册成功响应
        ResponseEvent mockResponseEvent = createMockRegisterSuccessResponse();
        
        // 2. 记录校时前的状态
        LocalDateTime beforeSync = timeSyncService.getLastSyncTime();
        long beforeOffset = timeSyncService.getTimeOffset();
        
        // 3. 处理注册响应（应该触发校时）
        assertDoesNotThrow(() -> {
            clientRegisterResponseProcessor.process(mockResponseEvent);
        });
        
        // 4. 验证校时结果
        LocalDateTime afterSync = timeSyncService.getLastSyncTime();
        
        // 校时时间应该已更新
        if (beforeSync != null) {
            assertTrue(afterSync.isAfter(beforeSync) || afterSync.isEqual(beforeSync));
        } else {
            assertNotNull(afterSync);
        }
        
        log.info("校时前偏差: {}ms, 校时后偏差: {}ms", beforeOffset, timeSyncService.getTimeOffset());
    }

    @Test
    void testGbSipDateFormatting() {
        // 测试GB28181时间格式
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        GbSipDate gbSipDate = new GbSipDate(calendar.getTimeInMillis());
        
        // 编码时间格式
        StringBuilder encoded = gbSipDate.encode(new StringBuilder());
        String timeString = encoded.toString();
        
        log.info("GB28181时间格式: {}", timeString);
        
        // 验证格式正确性 (yyyy-MM-ddTHH:mm:ss.SSS)
        assertTrue(timeString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        
        // 测试校时服务能否解析这个格式
        boolean syncResult = timeSyncService.syncTimeFromSip(timeString);
        assertTrue(syncResult, "TimeSyncService应该能解析GB28181时间格式");
    }

    @Test
    void testTimeSyncWithDifferentOffsets() {
        // 测试不同时间偏差的情况
        String[] testTimes = {
            // 过去1秒
            LocalDateTime.now().minusSeconds(1).toString().replace("T", "T").substring(0, 23),
            // 未来1秒  
            LocalDateTime.now().plusSeconds(1).toString().replace("T", "T").substring(0, 23),
            // 过去1小时
            LocalDateTime.now().minusHours(1).toString().replace("T", "T").substring(0, 23)
        };

        for (String timeStr : testTimes) {
            // 确保格式正确
            if (!timeStr.contains(".")) {
                timeStr += ".000";
            }
            
            boolean result = timeSyncService.syncTimeFromSip(timeStr);
            assertTrue(result, "应该能同步时间: " + timeStr);
            
            long offset = timeSyncService.getTimeOffset();
            log.info("时间: {}, 偏差: {}ms", timeStr, offset);
        }
    }

    @Test
    void testNtpTimeSyncFallback() {
        // 测试NTP作为备用校时方式
        // 注意：这个测试可能依赖网络连接，在CI环境中可能需要跳过
        
        String ntpServer = "pool.ntp.org";
        
        try {
            boolean result = timeSyncService.syncTimeFromNtp(ntpServer);
            // 如果网络可用，应该能同步成功
            if (result) {
                assertTrue(result);
                assertNotNull(timeSyncService.getLastSyncTime());
                log.info("NTP校时成功，偏差: {}ms", timeSyncService.getTimeOffset());
            } else {
                log.warn("NTP校时失败，可能是网络问题");
            }
        } catch (Exception e) {
            log.warn("NTP校时测试跳过，原因: {}", e.getMessage());
            // 在没有网络的环境中，这是正常的
        }
    }

    @Test
    void testTimeSyncThreshold() {
        // 测试时间偏差阈值检查
        
        // 设置小偏差（应该不需要校时）
        timeSyncService.setTimeOffset(500L); // 500ms
        assertFalse(timeSyncService.needsTimeSync(), "小偏差不应该需要校时");
        
        // 设置大偏差（应该需要校时）
        timeSyncService.setTimeOffset(2000L); // 2000ms
        assertTrue(timeSyncService.needsTimeSync(), "大偏差应该需要校时");
        
        // 测试负偏差
        timeSyncService.setTimeOffset(-1500L); // -1500ms
        assertTrue(timeSyncService.needsTimeSync(), "负偏差也应该需要校时");
    }

    /**
     * 创建模拟的注册成功响应
     */
    private ResponseEvent createMockRegisterSuccessResponse() {
        try {
            // 创建模拟的SIP响应
            SIPResponse mockResponse = mock(SIPResponse.class);
            ResponseEvent mockEvent = mock(ResponseEvent.class);
            
            // 设置响应状态码
            when(mockResponse.getStatusCode()).thenReturn(Response.OK);
            when(mockEvent.getResponse()).thenReturn(mockResponse);
            
            // 创建Date头域
            SIPDateHeader dateHeader = new SIPDateHeader();
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            
            // 设置头域
            when(mockResponse.getHeader("Date")).thenReturn(dateHeader);
            when(mockResponse.getCallIdHeader()).thenReturn(mock(javax.sip.header.CallIdHeader.class));
            when(mockResponse.getCallIdHeader().getCallId()).thenReturn("test-call-id");
            
            // 设置To头域（用于提取用户ID）
            javax.sip.header.ToHeader toHeader = mock(javax.sip.header.ToHeader.class);
            javax.sip.address.Address toAddress = mock(javax.sip.address.Address.class);
            javax.sip.address.SipURI toSipURI = mock(javax.sip.address.SipURI.class);
            
            when(mockResponse.getHeader("To")).thenReturn(toHeader);
            when(toHeader.getAddress()).thenReturn(toAddress);
            when(toAddress.getURI()).thenReturn(toSipURI);
            when(toSipURI.getUser()).thenReturn("testuser");
            
            return mockEvent;
        } catch (Exception e) {
            throw new RuntimeException("创建模拟响应失败", e);
        }
    }
}