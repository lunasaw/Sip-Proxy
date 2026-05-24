package io.github.lunasaw.gbproxy.client.transmit.request.message.handler;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 基础消息客户端处理器（兜底/示例用途，cmdType=Catalog）。
 *
 * <p>v1.5.0 改造：删除旧 {@code MessageRequestHandler} 构造器入参。本 handler 与
 * {@code CatalogQueryMessageClientHandler} cmdType 重复，由后注册者胜出（pre-existing 现象）。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class BaseMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";

    private String cmdType = CMD_TYPE;

    @Override
    public void handForEvt(RequestEvent event) {
        log.info("处理基础消息事件: event = {}", event);
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
