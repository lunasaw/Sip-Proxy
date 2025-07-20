package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
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
    public String getCommandType() {
        return "ACK";
    }

    @Override
    public String getCommandDescription() {
        return "ACK请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送ACK请求
        if (req.getContent() != null) {
            return SipSender.doAckRequest(req.getFromDevice(), req.getToDevice(), req.getContent(),
                    req.getContent(), req.getErrorEvent(), req.getOkEvent());
        } else {
            return SipSender.doAckRequest(req.getFromDevice(), req.getToDevice());
        }
    }
}