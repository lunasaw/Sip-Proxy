package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.SDCardStatusQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSdCardStatusQueryEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §9.5 / A.2.4.14 存储卡状态查询请求 (cmdType=SDCardStatus, rootType=Query)
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class SdCardStatusQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.SD_CARD_STATUS.getType();

    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    public SdCardStatusQueryMessageClientHandler(MessageRequestHandler messageRequestHandler,
                                                 ApplicationEventPublisher publisher) {
        super(messageRequestHandler);
        this.publisher = publisher;
    }

    @Override
    public String getRootType() {
        return QUERY;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            SDCardStatusQuery query = parseXml(SDCardStatusQuery.class);
            publisher.publishEvent(new ClientSdCardStatusQueryEvent(this, deviceSession.getUserId(), deviceSession.getSipId(), query));
        } catch (Exception e) {
            log.error("处理存储卡状态查询请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
