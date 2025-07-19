package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * SUBSCRIBE消息类型策略实现
 * 处理SUBSCRIBE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class SubscribeCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() {
        return "SUBSCRIBE";
    }

    @Override
    public String getCommandDescription() {
        return "SUBSCRIBE请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送SUBSCRIBE请求
        return SipSender.doSubscribeRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }
}