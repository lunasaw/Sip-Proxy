package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * BYE消息类型策略实现
 * 处理BYE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ByeCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // BYE命令通常不需要内容
        return null;
    }

    @Override
    public String getCommandType() {
        return "BYE";
    }

    @Override
    public String getCommandDescription() {
        return "BYE请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送BYE请求
        return SipSender.doByeRequest(fromDevice, toDevice, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // BYE命令不需要特殊参数验证
    }
}