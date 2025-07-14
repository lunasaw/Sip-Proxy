package io.github.lunasaw.gbproxy.client.transmit.request.message.handler;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 基础消息客户端处理器
 * 提供基础的消息处理功能
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class BaseMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";

    private String cmdType = CMD_TYPE;

    public BaseMessageClientHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public void handForEvt(RequestEvent event) {
        log.info("处理基础消息事件: event = {}", event);
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
