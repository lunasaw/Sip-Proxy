package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * ACK命令策略实现
 * 处理ACK响应相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class AckCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() {
        return "ACK";
    }

    @Override
    public String getCommandDescription() {
        return "ACK响应";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        return SipSender.doAckRequest(fromDevice, toDevice, content, null, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);

    }
}