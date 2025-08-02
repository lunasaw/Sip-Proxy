package io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.impl;

import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.AbstractServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transmit.cmd.strategy.ServerCommandStrategyReq;
import io.github.lunasaw.sip.common.transmit.SipSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * REGISTER消息类型策略实现
 * 处理REGISTER请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class RegisterCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() {
        return "REGISTER";
    }

    @Override
    public String getCommandDescription() {
        return "REGISTER请求";
    }

    @Override
    protected String sendCommand(ServerCommandStrategyReq req) {
        // 发送REGISTER请求
        Integer expire = Optional.ofNullable(req.getParamMap().get("expire")).map(Object::toString).map(Integer::valueOf).orElse(32);
        return SipSender.doRegisterRequest(req.getFromDevice(), req.getToDevice(), expire, req.getContent(), req.getErrorEvent(), req.getOkEvent());
    }
}