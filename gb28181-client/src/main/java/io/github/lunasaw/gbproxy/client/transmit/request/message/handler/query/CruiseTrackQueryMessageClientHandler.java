package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientCruiseTrackQueryEvent;
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
 * GB28181-2022 §9.5 / A.2.4.12 巡航轨迹查询请求 (cmdType=CruiseTrackQuery)
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class CruiseTrackQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.CRUISE_TRACK_QUERY.getType();

    private String cmdType = CMD_TYPE;
    private final ApplicationEventPublisher publisher;

    public CruiseTrackQueryMessageClientHandler(MessageRequestHandler messageRequestHandler,
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
            CruiseTrackQuery query = parseXml(CruiseTrackQuery.class);
            publisher.publishEvent(new ClientCruiseTrackQueryEvent(this, deviceSession.getUserId(), deviceSession.getSipId(), query));
        } catch (Exception e) {
            log.error("处理巡航轨迹查询请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
