package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
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
    public String getCommandType() {
        return "BYE";
    }

    @Override
    public String getCommandDescription() {
        return "BYE请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送BYE请求
        return SipSender.doByeRequest(req.getFromDevice(), req.getToDevice(), req.getErrorEvent(), req.getOkEvent());
    }
}