package io.github.lunasaw.sipgateway.core.core;

import com.alibaba.fastjson2.JSON;

/**
 * Payload 编解码工具：fastjson2 二次反序列化封装。
 *
 * @author luna
 */
public final class PayloadCodec {

    private PayloadCodec() {}

    /**
     * 将 payload 中的值转换为目标类型。
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object raw, Class<T> targetType) {
        if (raw == null) {
            return null;
        }
        if (targetType.isInstance(raw)) {
            return (T) raw;
        }
        // 数字类型转换
        if (raw instanceof Number num) {
            if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(num.intValue());
            }
            if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(num.longValue());
            }
            if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(num.doubleValue());
            }
        }
        // 枚举 / 复杂对象：用 fastjson2 反序列化
        return JSON.to(targetType, raw);
    }
}
