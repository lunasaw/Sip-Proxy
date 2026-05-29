package io.github.lunasaw.sipgateway.gb28181.dto;

import lombok.Data;

/**
 * INVITE 异步回包请求 DTO。
 *
 * <p>GB28181 设备主动 INVITE 时业务方需异步准备 SDP，通过协议特殊端点回包：
 * {@code POST /gateway/gb28181/invite/response}
 *
 * @author luna
 */
@Data
public class InviteResponseRequest {
    private String callId;
    private String sdp;
    /** 状态码，默认 200。预留 4xx/5xx 拒绝场景 */
    private Integer statusCode;
}
