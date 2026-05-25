package io.github.lunasaw.gbproxy.server.api.dto;

/**
 * 设备 INFO 请求 payload（cmdType=DeviceInfo 的非应答路径），由
 * {@code ServerInfoRequestProcessor} 在收到 INFO 时发布到 {@code ServerQueryResponseEvent}。
 *
 * @param content INFO 请求 body（原始字符串）
 * @author luna
 */
public record DeviceInfoRequest(String content) {
}
