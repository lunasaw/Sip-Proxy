package io.github.lunasaw.sipgateway.core.api;

/**
 * 参数绑定 DSL。
 *
 * <p>示例：
 * <ul>
 *   <li>"deviceId"                          → cmd.deviceId()
 *   <li>"callId"                            → payload.callId（顶层别名 callId 优先）
 *   <li>"interval"                          → payload.interval as String
 *   <li>"expires:int"                       → payload.expires 转 Integer
 *   <li>"speed:int?128"                     → payload.speed 转 Integer，缺省 128
 *   <li>"streamMode:StreamModeEnum?UDP"     → JSON.to(StreamModeEnum, payload.streamMode)，缺省 UDP
 *   <li>"osdInfo:OsdConfig$OsdInfo"         → JSON.to(嵌套 class, payload.osdInfo)
 *   <li>"dragZoom:DragZoom"                 → JSON.to(DragZoom, payload.dragZoom)
 * </ul>
 *
 * @param source       "deviceId" | "callId" | "payload.<field>"
 * @param fieldName    字段名（payload 取值用）
 * @param targetType   目标类型，反射调用前转换
 * @param defaultValue 缺省值，null 表示必填（缺失抛 400）
 * @author luna
 */
public record ParamBinding(
        String source,
        String fieldName,
        Class<?> targetType,
        Object defaultValue
) {
    /**
     * 工厂方法：解析 DSL 字符串为 ParamBinding。
     * 格式："fieldName" 或 "fieldName:type" 或 "fieldName:type?default"
     */
    public static ParamBinding parse(String dsl) {
        // deviceId / callId 特殊处理
        if ("deviceId".equals(dsl)) {
            return new ParamBinding("deviceId", "deviceId", String.class, null);
        }
        if ("callId".equals(dsl)) {
            return new ParamBinding("callId", "callId", String.class, null);
        }

        // 解析 fieldName:type?default
        String[] parts = dsl.split("\\?", 2);
        String fieldPart = parts[0];
        Object defaultValue = parts.length > 1 ? parseDefault(parts[1]) : null;

        String[] typeParts = fieldPart.split(":", 2);
        String fieldName = typeParts[0];
        Class<?> targetType = typeParts.length > 1 ? parseType(typeParts[1]) : String.class;

        return new ParamBinding("payload", fieldName, targetType, defaultValue);
    }

    private static Class<?> parseType(String typeToken) {
        switch (typeToken) {
            case "int":
                return Integer.class;
            case "long":
                return Long.class;
            case "double":
                return Double.class;
            case "boolean":
                return Boolean.class;
            default:
                try {
                    // 尝试加载类（支持枚举、嵌套类等）
                    return Class.forName(typeToken.replace("$", "."));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown type: " + typeToken, e);
                }
        }
    }

    private static Object parseDefault(String defaultStr) {
        // 简单实现：数字直接解析，字符串保持原样
        try {
            return Integer.parseInt(defaultStr);
        } catch (NumberFormatException e) {
            return defaultStr;
        }
    }
}
