package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
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
public class InviteCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // INVITE命令需要构建SDP内容
        if (params.length > 0 && params[0] instanceof String) {
            return (String) params[0];
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
        // INVITE命令需要SDP内容参数
        if (params.length == 0 || !(params[0] instanceof String)) {
            throw new IllegalArgumentException("INVITE命令需要提供SDP内容参数");
        }
    }
}