package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
import lombok.extern.slf4j.Slf4j;

/**
 * INFO消息类型策略实现
 * 处理INFO请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class InfoCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() {
        return "INFO";
    }

    @Override
    public String getCommandDescription() {
        return "INFO请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送INFO请求
        return SipSender.doInfoRequest(req.getFromDevice(), req.getToDevice(), req.getContent(), req.getErrorEvent(), req.getOkEvent());
    }
}