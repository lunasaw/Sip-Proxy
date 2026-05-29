package io.github.lunasaw.sipgateway.core.api.envelope;

/**
 * Gateway 出站命令结果 envelope（gateway → 业务）。
 *
 * @param correlationId 关联键：sn（Query/Subscribe）或 callId（Invite/Bye）。业务侧用此键关联回调
 * @param type          命令类型，回执给业务侧确认无歧义
 * @param nodeId        处理节点（多节点部署排查用）
 * @author luna
 */
public record GatewayCommandResult(
        String correlationId,
        String type,
        String nodeId
) {}
