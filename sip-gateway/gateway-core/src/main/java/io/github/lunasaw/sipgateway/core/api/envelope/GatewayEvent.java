package io.github.lunasaw.sipgateway.core.api.envelope;

import java.util.Map;

/**
 * Gateway 入站事件 envelope（gateway → 业务）。
 *
 * @param type          事件类型：protocol.Group.Name 三段式，如 "gb28181.Lifecycle.Online"
 * @param deviceId      设备 GB28181 编码（部分 Session.* 事件没有 deviceId，仅有 callId）
 * @param correlationId 关联键：sn（Response.*）或 callId（Session.*），不适用时 null
 * @param timestampMs   事件发生时间（毫秒，gateway 节点时钟）
 * @param payload       事件载荷（Map<String,Object>，业务侧按 type 反序列化）
 * @param nodeId        gateway 节点标识
 * @author luna
 */
public record GatewayEvent(
        String type,
        String deviceId,
        String correlationId,
        long timestampMs,
        Map<String, Object> payload,
        String nodeId
) {}
