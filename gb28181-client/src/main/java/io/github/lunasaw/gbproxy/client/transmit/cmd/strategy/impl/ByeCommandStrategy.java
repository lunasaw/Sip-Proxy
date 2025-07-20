package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * BYE命令策略实现
 * 处理BYE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ByeCommandStrategy extends AbstractClientCommandStrategy {

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
        // 直接发送BYE请求，不通过MESSAGE
        return SipSender.doByeRequest(fromDevice, toDevice);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // BYE命令不需要额外参数校验
    }
}