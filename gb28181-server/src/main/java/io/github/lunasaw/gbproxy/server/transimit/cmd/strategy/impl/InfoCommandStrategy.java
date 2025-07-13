package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * INFO消息类型策略实现
 * 处理INFO请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class InfoCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // INFO命令通常包含内容
        if (params.length > 0 && params[0] instanceof String) {
            return (String) params[0];
        }
        return null;
    }

    @Override
    public String getCommandType() {
        return "INFO";
    }

    @Override
    public String getCommandDescription() {
        return "INFO请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送INFO请求
        return SipSender.doInfoRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // INFO命令需要内容参数
        if (params.length == 0 || params[0] == null) {
            throw new IllegalArgumentException("INFO命令需要提供内容参数");
        }
    }
}