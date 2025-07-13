package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * INVITE消息类型策略实现
 * 处理INVITE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class InviteCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // INVITE命令通常包含SDP内容
        if (params.length > 0) {
            Object param = params[0];
            if (param instanceof String) {
                return (String) param;
            }
        }
        return null;
    }

    @Override
    public String getCommandType() {
        return "INVITE";
    }

    @Override
    public String getCommandDescription() {
        return "INVITE请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送INVITE请求
        return SipSender.doInviteRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // INVITE命令需要内容参数
        if (params.length == 0 || params[0] == null) {
            throw new IllegalArgumentException("INVITE命令需要提供内容参数");
        }
    }
}