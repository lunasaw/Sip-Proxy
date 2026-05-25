package io.github.lunasaw.gbproxy.server.api.dto;

/**
 * 设备 INFO 处理错误 payload，由 {@code ServerInfoRequestProcessor} 在异常路径发布到
 * {@code ServerQueryResponseEvent}。
 *
 * @param reason 错误描述
 * @author luna
 */
public record DeviceInfoError(String reason) {
}
