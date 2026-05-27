package io.github.lunasaw.gbproxy.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * sip-gateway 节点配置。
 *
 * <p>对应 LAYERED-ARCHITECTURE.md §6.5：节点标识 + nodeAddressMap 装配。
 * 多节点部署时 {@link #nodeId} 必须与 {@link #nodes} 中本节点的 key 一致，
 * 否则跨节点 INVITE 回包路由会找不到目标。
 *
 * <p>开发 / 小规模部署用静态配置；生产建议改为基于 K8s Endpoints 或 Nacos/Consul
 * 的动态服务发现，但 Bean 名仍为 {@code nodeAddressMap} 以保持注入兼容。
 *
 * <p>配置前缀：{@code gb28181.gateway.*}（v1.8.0 起）。
 *
 * @author luna
 */
@Data
@ConfigurationProperties("gb28181.gateway")
public class GatewayProperties {

    /**
     * 当前节点标识。Redis 中 {@code sip:invite:ctx:{callId}} 的 value 前缀使用此值。
     */
    private String nodeId = "node-default";

    /**
     * nodeId → 节点内网地址（含 scheme、host、port）。
     * 单机部署留空即可，所有 INVITE 回包都走本节点。
     */
    private Map<String, String> nodes = new HashMap<>();

    /**
     * INVITE 上下文在共享存储（Redis / 内存）中的存活时间，毫秒。
     * 默认 30s 与 RFC 3261 Timer B 对齐。
     */
    private long inviteContextTtlMs = 30_000L;

    /**
     * INVITE 重传幂等窗口，毫秒。UDP 下设备会按 T1 退避重传，
     * 在此窗口内同一 callId 只会向业务服务器推送一次。
     */
    private long inviteIdempotencyWindowMs = 60_000L;
}
