package io.github.lunasaw.gbproxy.test.gateway.dto;

import lombok.Data;

/**
 * 业务服务器准备好 SDP 后回包请求体。callId 透传给 sip-gateway 即可，
 * 不需要 contextKey——gateway 会通过 {@code InviteContextStore} 反查。
 */
@Data
public class InviteResponseRequest {
    private String callId;
    private String sdp;
}
