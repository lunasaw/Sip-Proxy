package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
import lombok.extern.slf4j.Slf4j;

/**
 * MESSAGE消息类型策略实现
 * 处理MESSAGE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class MessageCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() {
        return "MESSAGE";
    }

    @Override
    public String getCommandDescription() {
        return "MESSAGE请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送MESSAGE请求
        return SipSender.doMessageRequest(req.getFromDevice(), req.getToDevice(), req.getContent(), req.getErrorEvent(), req.getOkEvent());
    }
}