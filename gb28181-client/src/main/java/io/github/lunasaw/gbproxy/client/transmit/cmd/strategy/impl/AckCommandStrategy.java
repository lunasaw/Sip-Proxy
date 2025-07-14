package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * ACK命令策略实现
 * 处理ACK响应相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class AckCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // ACK命令不需要构建内容，直接发送ACK响应
        return null;
    }

    @Override
    public String getCommandType() {
        return "ACK";
    }

    @Override
    public String getCommandDescription() {
        return "ACK响应";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 根据参数数量决定调用哪个ACK方法
        if (params.length == 0) {
            return SipSender.doAckRequest(fromDevice, toDevice);
        } else if (params.length == 1) {
            String callId = (String) params[0];
            return SipSender.doAckRequest(fromDevice, toDevice, callId);
        } else if (params.length == 2) {
            String contentParam = (String) params[0];
            String callId = (String) params[1];
            return SipSender.doAckRequest(fromDevice, toDevice, contentParam, callId);
        } else {
            throw new IllegalArgumentException("ACK命令参数数量不正确");
        }
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);

        // ACK命令参数校验
        if (params.length > 2) {
            throw new IllegalArgumentException("ACK命令最多支持2个参数");
        }

        if (params.length > 0) {
            if (!(params[0] instanceof String)) {
                throw new IllegalArgumentException("ACK命令的第一个参数必须是String类型");
            }
        }

        if (params.length > 1) {
            if (!(params[1] instanceof String)) {
                throw new IllegalArgumentException("ACK命令的第二个参数必须是String类型");
            }
        }
    }
}