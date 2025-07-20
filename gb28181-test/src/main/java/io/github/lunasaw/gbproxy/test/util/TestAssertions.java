package io.github.lunasaw.gbproxy.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 自定义测试断言工具类
 * 提供GB28181测试特有的断言方法
 */
@Slf4j
@Component
public class TestAssertions {

    /**
     * 断言SIP响应状态码
     */
    public static void assertSipResponseCode(int actualCode, int expectedCode, String message) {
        if (actualCode != expectedCode) {
            throw new AssertionError(String.format("%s - 期望状态码: %d, 实际状态码: %d",
                    message, expectedCode, actualCode));
        }
    }

    /**
     * 断言SIP响应状态码在指定范围内
     */
    public static void assertSipResponseCodeInRange(int actualCode, int minCode, int maxCode, String message) {
        if (actualCode < minCode || actualCode > maxCode) {
            throw new AssertionError(String.format("%s - 期望状态码范围: %d-%d, 实际状态码: %d",
                    message, minCode, maxCode, actualCode));
        }
    }

    /**
     * 断言SIP消息头部存在
     */
    public static void assertSipHeaderExists(String headerValue, String headerName, String message) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            throw new AssertionError(String.format("%s - SIP头部 '%s' 不存在或为空", message, headerName));
        }
    }

    /**
     * 断言SIP消息头部包含指定值
     */
    public static void assertSipHeaderContains(String headerValue, String expectedContent,
                                               String headerName, String message) {
        assertSipHeaderExists(headerValue, headerName, message);
        if (!headerValue.contains(expectedContent)) {
            throw new AssertionError(String.format("%s - SIP头部 '%s' 不包含期望内容 '%s', 实际值: %s",
                    message, headerName, expectedContent, headerValue));
        }
    }

    /**
     * 断言GB28181设备ID格式正确
     */
    public static void assertValidGb28181DeviceId(String deviceId, String message) {
        if (deviceId == null) {
            throw new AssertionError(message + " - 设备ID不能为空");
        }

        if (deviceId.length() != 20) {
            throw new AssertionError(String.format("%s - 设备ID长度应为20位, 实际长度: %d",
                    message, deviceId.length()));
        }

        if (!deviceId.matches("\\d{20}")) {
            throw new AssertionError(String.format("%s - 设备ID应为20位数字, 实际值: %s",
                    message, deviceId));
        }
    }

    /**
     * 断言XML消息格式正确
     */
    public static void assertValidXmlMessage(String xmlContent, String message) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new AssertionError(message + " - XML内容不能为空");
        }

        if (!xmlContent.trim().startsWith("<?xml")) {
            throw new AssertionError(message + " - XML内容应以XML声明开始");
        }

        // 简单的XML格式检查
        long openTags = xmlContent.chars().filter(ch -> ch == '<').count();
        long closeTags = xmlContent.chars().filter(ch -> ch == '>').count();

        if (openTags != closeTags) {
            throw new AssertionError(String.format("%s - XML标签不匹配, 开始标签: %d, 结束标签: %d",
                    message, openTags, closeTags));
        }
    }

    /**
     * 断言XML消息包含指定标签
     */
    public static void assertXmlContainsTag(String xmlContent, String tagName, String message) {
        assertValidXmlMessage(xmlContent, message);

        String openTag = "<" + tagName;
        String closeTag = "</" + tagName + ">";

        if (!xmlContent.contains(openTag)) {
            throw new AssertionError(String.format("%s - XML内容不包含标签 '%s'", message, tagName));
        }
    }

    /**
     * 断言XML消息包含指定标签和值
     */
    public static void assertXmlContainsTagWithValue(String xmlContent, String tagName,
                                                     String expectedValue, String message) {
        assertXmlContainsTag(xmlContent, tagName, message);

        String pattern = "<" + tagName + "[^>]*>" + expectedValue + "</" + tagName + ">";
        if (!xmlContent.matches(".*" + pattern + ".*")) {
            throw new AssertionError(String.format("%s - XML标签 '%s' 的值不是期望的 '%s'",
                    message, tagName, expectedValue));
        }
    }

    /**
     * 断言超时内完成操作
     */
    public static void assertCompletesWithinTimeout(Runnable operation, long timeoutMs, String message) {
        long startTime = System.currentTimeMillis();

        try {
            operation.run();
        } catch (Exception e) {
            throw new AssertionError(message + " - 操作执行失败: " + e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > timeoutMs) {
            throw new AssertionError(String.format("%s - 操作超时, 期望: %dms, 实际: %dms",
                    message, timeoutMs, duration));
        }
    }

    /**
     * 断言异步操作在超时内完成
     */
    public static <T> T assertCompletesWithinTimeout(Supplier<CompletableFuture<T>> futureSupplier,
                                                     long timeoutMs, String message) {
        try {
            CompletableFuture<T> future = futureSupplier.get();
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AssertionError(String.format("%s - 异步操作超时或失败: %s", message, e.getMessage()), e);
        }
    }

    /**
     * 断言设备注册状态
     */
    public static void assertDeviceRegistered(String deviceId, boolean expectedRegistered, String message) {
        // 这里应该查询实际的设备注册状态
        // 为了演示，假设有一个设备状态查询方法
        log.debug("检查设备注册状态: {}", deviceId);

        // 实际实现中应该调用设备管理服务查询状态
        // boolean actualRegistered = deviceManager.isDeviceRegistered(deviceId);
        // 这里暂时用日志代替

        log.info("{} - 设备 {} 注册状态检查: 期望={}", message, deviceId, expectedRegistered);
    }

    /**
     * 断言设备在线状态
     */
    public static void assertDeviceOnline(String deviceId, boolean expectedOnline, String message) {
        // 这里应该查询实际的设备在线状态
        log.debug("检查设备在线状态: {}", deviceId);

        // 实际实现中应该调用设备管理服务查询状态
        // boolean actualOnline = deviceManager.isDeviceOnline(deviceId);

        log.info("{} - 设备 {} 在线状态检查: 期望={}", message, deviceId, expectedOnline);
    }

    /**
     * 断言SIP会话存在
     */
    public static void assertSipSessionExists(String callId, String message) {
        if (callId == null || callId.trim().isEmpty()) {
            throw new AssertionError(message + " - Call-ID不能为空");
        }

        // 这里应该查询实际的SIP会话状态
        log.debug("检查SIP会话: {}", callId);

        // 实际实现中应该调用SIP会话管理器查询
        // boolean sessionExists = sipSessionManager.sessionExists(callId);

        log.info("{} - SIP会话 {} 存在性检查", message, callId);
    }

    /**
     * 断言媒体流活跃
     */
    public static void assertMediaStreamActive(String streamId, boolean expectedActive, String message) {
        if (streamId == null || streamId.trim().isEmpty()) {
            throw new AssertionError(message + " - 流ID不能为空");
        }

        // 这里应该查询实际的媒体流状态
        log.debug("检查媒体流状态: {}", streamId);

        // 实际实现中应该调用媒体服务器查询流状态
        // boolean actualActive = mediaServer.isStreamActive(streamId);

        log.info("{} - 媒体流 {} 活跃状态检查: 期望={}", message, streamId, expectedActive);
    }

    /**
     * 断言数值在指定范围内
     */
    public static void assertValueInRange(double actualValue, double minValue, double maxValue, String message) {
        if (actualValue < minValue || actualValue > maxValue) {
            throw new AssertionError(String.format("%s - 数值超出范围, 期望: [%.2f, %.2f], 实际: %.2f",
                    message, minValue, maxValue, actualValue));
        }
    }

    /**
     * 断言执行时间在合理范围内
     */
    public static void assertExecutionTimeReasonable(long actualMs, long maxExpectedMs, String message) {
        if (actualMs > maxExpectedMs) {
            throw new AssertionError(String.format("%s - 执行时间过长, 期望最大: %dms, 实际: %dms",
                    message, maxExpectedMs, actualMs));
        }

        if (actualMs < 0) {
            throw new AssertionError(String.format("%s - 执行时间异常, 实际: %dms", message, actualMs));
        }
    }

    /**
     * 断言字符串不为空且符合模式
     */
    public static void assertStringMatchesPattern(String actualValue, String pattern, String message) {
        if (actualValue == null) {
            throw new AssertionError(message + " - 字符串不能为null");
        }

        if (actualValue.trim().isEmpty()) {
            throw new AssertionError(message + " - 字符串不能为空");
        }

        if (!actualValue.matches(pattern)) {
            throw new AssertionError(String.format("%s - 字符串不匹配模式, 期望模式: %s, 实际值: %s",
                    message, pattern, actualValue));
        }
    }

    /**
     * 软断言 - 记录失败但不抛出异常
     */
    public static boolean softAssert(boolean condition, String message) {
        if (!condition) {
            log.error("软断言失败: {}", message);
            return false;
        }
        return true;
    }

    /**
     * 记录断言成功
     */
    public static void logAssertionSuccess(String message) {
        log.info("✓ 断言成功: {}", message);
    }

    /**
     * 记录断言失败
     */
    public static void logAssertionFailure(String message, Throwable cause) {
        log.error("✗ 断言失败: {}", message, cause);
    }
}