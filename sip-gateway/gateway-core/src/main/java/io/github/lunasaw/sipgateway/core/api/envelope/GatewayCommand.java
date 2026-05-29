package io.github.lunasaw.sipgateway.core.api.envelope;

import java.util.Map;

/**
 * Gateway 出站命令 envelope（业务 → gateway）。
 *
 * @param type      命令类型：protocol.Group.Name 三段式，如 "gb28181.Query.Catalog"
 * @param deviceId  设备 GB28181 编码（INVITE 回包等基于 callId 的命令该字段为 null）
 * @param payload   协议参数，按 type 对应的 schema 填值
 * @param requestId 业务侧追踪 ID，gateway 透传到回调 envelope 的 traceId 字段
 * @author luna
 */
public record GatewayCommand(
        String type,
        String deviceId,
        Map<String, Object> payload,
        String requestId
) {
    public GatewayCommand withType(String newType) {
        return new GatewayCommand(newType, deviceId, payload, requestId);
    }
}
