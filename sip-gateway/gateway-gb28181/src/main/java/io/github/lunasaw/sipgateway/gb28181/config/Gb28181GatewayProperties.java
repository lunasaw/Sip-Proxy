package io.github.lunasaw.sipgateway.gb28181.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GB28181 网关协议子前缀配置。
 *
 * @author luna
 */
@Data
@ConfigurationProperties(prefix = "gateway.gb28181")
public class Gb28181GatewayProperties {

    /**
     * INVITE 上下文 TTL（毫秒）：超时后业务侧回包返回 410。
     */
    private long inviteContextTtlMs = 30_000L;

    /**
     * INVITE 重传幂等窗口（毫秒）。UDP 下设备会按 T1 退避重传 INVITE，按 callId 在窗口内幂等。
     */
    private long inviteIdempotencyWindowMs = 5_000L;
}
