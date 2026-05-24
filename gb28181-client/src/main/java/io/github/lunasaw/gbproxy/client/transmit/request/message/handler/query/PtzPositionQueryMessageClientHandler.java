package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.PTZPositionQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientPtzPositionQueryEvent;
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
 * GB28181-2022 §9.5 / A.2.4.13 PTZ 精确状态查询请求 (cmdType=PTZPosition, rootType=Query)
 *
 * 客户端收到平台查询 → 发布 {@link ClientPtzPositionQueryEvent}，
 * 业务方在 {@code @EventListener} 中调用 {@link io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender#sendPtzPositionResponse}
 * 返回应答。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class PtzPositionQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.PTZ_POSITION.getType();

    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    public PtzPositionQueryMessageClientHandler(MessageRequestHandler messageRequestHandler,
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
            PTZPositionQuery query = parseXml(PTZPositionQuery.class);
            publisher.publishEvent(new ClientPtzPositionQueryEvent(this, deviceSession.getUserId(), deviceSession.getSipId(), query));
        } catch (Exception e) {
            log.error("处理 PTZ 精确状态查询请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
