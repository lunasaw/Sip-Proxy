package io.github.lunasaw.sip.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceUtils测试类
 *
 * @author luna
 */
@Slf4j
class TraceUtilsTest {

    @BeforeEach
    void setUp() {
        // 确保每个测试前清理traceId
        TraceUtils.clearTraceId();
    }

    @AfterEach
    void tearDown() {
        // 确保每个测试后清理traceId
        TraceUtils.clearTraceId();
    }

    @Test
    void testSetAndGetTraceId() {
        // 设置traceId
        String traceId = "test-trace-123";
        TraceUtils.setTraceId(traceId);

        // 验证获取到的traceId
        assertEquals(traceId, TraceUtils.getTraceId());
        assertEquals(traceId, TraceUtils.getCurrentTraceId());

        // 验证MDC中是否设置
        assertEquals(traceId, MDC.get(TraceUtils.TRACE_ID_KEY));

        log.info("测试设置和获取traceId: {}", TraceUtils.getTraceId());
    }

    @Test
    void testAutoGenerateTraceId() {
        // 获取traceId（应该自动生成）
        String traceId = TraceUtils.getTraceId();

        // 验证traceId不为空且长度为16
        assertNotNull(traceId);
        assertEquals(16, traceId.length());

        // 验证MDC中是否设置
        assertEquals(traceId, MDC.get(TraceUtils.TRACE_ID_KEY));

        log.info("测试自动生成traceId: {}", traceId);
    }

    @Test
    void testClearTraceId() {
        // 设置traceId
        String traceId = "test-trace-456";
        TraceUtils.setTraceId(traceId);

        // 验证设置成功
        assertEquals(traceId, TraceUtils.getTraceId());

        // 清除traceId
        TraceUtils.clearTraceId();

        // 验证清除成功
        assertNull(TraceUtils.getCurrentTraceId());
        assertNull(MDC.get(TraceUtils.TRACE_ID_KEY));

        // 验证getTraceId会重新生成
        String newTraceId = TraceUtils.getTraceId();
        assertNotNull(newTraceId);
        assertNotEquals(traceId, newTraceId);

        log.info("测试清除traceId后重新生成: {}", newTraceId);
    }

    @Test
    void testHasTraceId() {
        // 初始状态应该没有traceId
        assertFalse(TraceUtils.hasTraceId());

        // 设置traceId后应该有
        TraceUtils.setTraceId("test-trace-789");
        assertTrue(TraceUtils.hasTraceId());

        // 清除后应该没有
        TraceUtils.clearTraceId();
        assertFalse(TraceUtils.hasTraceId());
    }

    @Test
    void testSetEmptyTraceId() {
        // 设置空traceId应该被忽略
        TraceUtils.setTraceId("");
        assertFalse(TraceUtils.hasTraceId());

        TraceUtils.setTraceId(null);
        assertFalse(TraceUtils.hasTraceId());

        TraceUtils.setTraceId("   ");
        assertFalse(TraceUtils.hasTraceId());
    }

    @Test
    void testGenerateTraceId() {
        String traceId1 = TraceUtils.generateTraceId();
        String traceId2 = TraceUtils.generateTraceId();

        // 验证生成的traceId格式
        assertNotNull(traceId1);
        assertNotNull(traceId2);
        assertEquals(16, traceId1.length());
        assertEquals(16, traceId2.length());

        // 验证每次生成的traceId都不同
        assertNotEquals(traceId1, traceId2);

        // 验证只包含十六进制字符
        assertTrue(traceId1.matches("[0-9a-f]{16}"));
        assertTrue(traceId2.matches("[0-9a-f]{16}"));

        log.info("测试生成traceId: {} vs {}", traceId1, traceId2);
    }


    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        // 在主线程设置traceId
        String mainThreadTraceId = "main-thread-trace";
        TraceUtils.setTraceId(mainThreadTraceId);

        // 创建子线程
        Thread childThread = new Thread(() -> {
            // 子线程应该没有traceId
            assertFalse(TraceUtils.hasTraceId());
            assertNull(TraceUtils.getCurrentTraceId());

            // 在子线程中设置traceId
            String childThreadTraceId = "child-thread-trace";
            TraceUtils.setTraceId(childThreadTraceId);

            // 验证子线程的traceId
            assertEquals(childThreadTraceId, TraceUtils.getTraceId());

            log.info("子线程traceId: {}", TraceUtils.getTraceId());
        });

        childThread.start();
        childThread.join();

        // 主线程的traceId应该保持不变
        assertEquals(mainThreadTraceId, TraceUtils.getTraceId());

        log.info("主线程traceId: {}", TraceUtils.getTraceId());
    }
}