package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * NOTIFY消息类型策略实现
 * 处理NOTIFY请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class NotifyCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // NOTIFY命令需要构建XML内容
        if (params.length > 0 && params[0] instanceof String) {
            return (String) params[0];
        }
        return null;
    }

    @Override
    public String getCommandType() {
        return "NOTIFY";
    }

    @Override
    public String getCommandDescription() {
        return "NOTIFY请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送NOTIFY请求
        return SipSender.doNotifyRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }
}