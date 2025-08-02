package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
import lombok.extern.slf4j.Slf4j;

/**
 * SUBSCRIBE消息类型策略实现
 * 处理SUBSCRIBE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class SubscribeCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() {
        return "SUBSCRIBE";
    }

    @Override
    public String getCommandDescription() {
        return "SUBSCRIBE请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送SUBSCRIBE请求
        return SipSender.doSubscribeRequest(req.getFromDevice(), req.getToDevice(), req.getContent(), req.getErrorEvent(), req.getOkEvent());
    }
}