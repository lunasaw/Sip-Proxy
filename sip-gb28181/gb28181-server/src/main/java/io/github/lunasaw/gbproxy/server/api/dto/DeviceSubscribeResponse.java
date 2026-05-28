package io.github.lunasaw.gbproxy.server.api.dto;

/**
 * 设备订阅响应 payload，由 {@code SubscribeResponseProcessor} 在收到 SUBSCRIBE 200/失败时发布到
 * {@code ServerQueryResponseEvent}。
 *
 * @param callId    SIP Call-ID
 * @param statusCode SIP 状态码（200/4xx/5xx）
 * @author luna
 */
public record DeviceSubscribeResponse(String callId, int statusCode) {
}
