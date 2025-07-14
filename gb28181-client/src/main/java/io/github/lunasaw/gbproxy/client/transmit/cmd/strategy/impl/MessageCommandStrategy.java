package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * MESSAGE消息类型策略实现
 * 处理MESSAGE请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class MessageCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // MESSAGE命令需要构建XML内容
        if (params.length > 0) {
            Object param = params[0];

            // 如果参数是字符串，直接返回
            if (param instanceof String) {
                return (String) param;
            }

            // 如果参数是对象，尝试转换为XML
            if (param != null) {
                try {
                    // 使用XmlUtils将对象序列化为XML
                    return XmlUtils.toString("UTF-8", param);
                } catch (Exception e) {
                    log.error("参数转换失败: {}", param, e);
                    throw new IllegalArgumentException("参数转换失败: " + param);
                }
            }
        }
        return null;
    }

    @Override
    public String getCommandType() {
        return "MESSAGE";
    }

    @Override
    public String getCommandDescription() {
        return "MESSAGE请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 发送MESSAGE请求
        return SipSender.doMessageRequest(fromDevice, toDevice, content, errorEvent, okEvent);
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // MESSAGE命令需要内容参数
        if (params.length == 0 || params[0] == null) {
            throw new IllegalArgumentException("MESSAGE命令需要提供内容参数");
        }
    }
}