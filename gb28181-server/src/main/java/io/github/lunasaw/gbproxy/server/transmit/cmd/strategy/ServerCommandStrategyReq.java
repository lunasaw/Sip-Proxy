package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端命令策略请求参数封装类
 *
 * @author luna
 * @date 2024/01/01
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerCommandStrategyReq {

    /**
     * 发送设备
     */
    private FromDevice fromDevice;

    /**
     * 接收设备
     */
    private ToDevice toDevice;

    /**
     * 命令内容
     */
    private String content;

    /**
     * 错误事件
     */
    private Event errorEvent;

    /**
     * 成功事件
     */
    private Event okEvent;

    /**
     * 方法参数映射
     */
    private Map<String, Object> paramMap = new ConcurrentHashMap<>();
}