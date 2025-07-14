package io.github.lunasaw.sip.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Trace跟踪工具类
 * 提供traceId的ThreadLocal管理和MDC集成功能
 *
 * @author luna
 */
@Slf4j
public class TraceUtils {

    /**
     * TraceId的MDC键名
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * TraceId的ThreadLocal存储
     */
    private static final ThreadLocal<String> TRACE_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 获取trace并启动跟踪
     * 生成新的traceId，启动跟踪，并返回当前traceId
     *
     * @return 当前traceId，如果不存在则返回空字符串
     */
    public static String getTrace() {
        String traceId = generateTraceId();
        genTrace(traceId);
        return Optional.ofNullable(getCurrentTraceId()).orElse("");
    }

    /**
     * 根据traceId启动跟踪
     * 如果传入的traceId为空，则使用当前traceId或生成新的traceId
     *
     * @param traceId 跟踪ID
     */
    public static void genTrace(String traceId) {
        if (StringUtils.isNotBlank(traceId)) {
            if (Objects.equals(getCurrentTraceId(), traceId)) {
                // 当前有相同的traceId，直接返回
                log.debug("当前已存在相同的traceId: {}", traceId);
                return;
            } else {
                // 启动新的跟踪
                startTrace(traceId, "SIP", "Scheduled");
            }
        } else {
            // traceId为空，使用当前traceId或生成新的
            String genTraceId = Optional.ofNullable(getCurrentTraceId()).orElse(generateTraceId());
            startTrace(genTraceId, "SIP", "Scheduled");
        }
        // 设置MDC
        MDC.put(TRACE_ID_KEY, Optional.ofNullable(traceId).orElse(getCurrentTraceId()));
    }

    /**
     * 启动跟踪
     *
     * @param traceId   跟踪ID
     * @param traceType 跟踪类型
     * @param source    来源
     */
    private static void startTrace(String traceId, String traceType, String source) {
        setTraceId(traceId);
        log.debug("启动跟踪 - traceId: {}, type: {}, source: {}", traceId, traceType, source);
    }

    /**
     * 设置traceId到ThreadLocal和MDC中
     *
     * @param traceId 跟踪ID
     */
    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.trim().isEmpty()) {
            log.warn("尝试设置空的traceId，已忽略");
            return;
        }

        TRACE_ID_THREAD_LOCAL.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
        log.debug("设置traceId: {}", traceId);
    }

    /**
     * 获取当前线程的traceId
     * 如果不存在则自动生成一个新的traceId并设置到MDC中
     *
     * @return 当前traceId
     */
    public static String getTraceId() {
        String traceId = TRACE_ID_THREAD_LOCAL.get();
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
            log.debug("自动生成并设置traceId: {}", traceId);
        }
        return traceId;
    }

    /**
     * 获取当前线程的traceId（不自动生成）
     *
     * @return 当前traceId，如果不存在则返回null
     */
    public static String getCurrentTraceId() {
        return TRACE_ID_THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的traceId
     */
    public static void clearTraceId() {
        String traceId = TRACE_ID_THREAD_LOCAL.get();
        TRACE_ID_THREAD_LOCAL.remove();
        MDC.remove(TRACE_ID_KEY);
        log.debug("清除traceId: {}", traceId);
    }

    /**
     * 生成新的traceId
     *
     * @return 生成的traceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 检查当前线程是否有traceId
     *
     * @return 如果存在traceId则返回true，否则返回false
     */
    public static boolean hasTraceId() {
        String traceId = TRACE_ID_THREAD_LOCAL.get();
        return traceId != null && !traceId.trim().isEmpty();
    }
}