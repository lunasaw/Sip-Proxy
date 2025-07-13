package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * ACK消息类型策略实现
 * 处理ACK请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class AckCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // ACK命令可能包含内容
        if (params.length > 0 && params[0] instanceof String) {
            return (String) params[0];
        }
        return null;
    }

    @Override
    public String getCommandType() {
        return "ACK";
    }

    @Override
    public String getCommandDescription() {
        return "ACK请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送ACK请求
        return SipSender.doAckRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // ACK命令不需要特殊参数验证
    }
}