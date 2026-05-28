package io.github.lunasaw.sipgateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 核心配置（协议中立）。
 *
 * @author luna
 */
@Data
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /**
     * 本节点 ID（多节点部署时必填，用于跨节点路由）。
     */
    private String nodeId = "default-node";

    /**
     * 节点地址映射表：nodeId → HTTP base URL。
     * 例如：{"node-1": "http://10.0.0.1:8080", "node-2": "http://10.0.0.2:8080"}
     */
    private Map<String, String> nodes = new HashMap<>();

    /**
     * 跨节点转发超时（毫秒）。
     */
    private long forwardTimeoutMs = 3000;
}
