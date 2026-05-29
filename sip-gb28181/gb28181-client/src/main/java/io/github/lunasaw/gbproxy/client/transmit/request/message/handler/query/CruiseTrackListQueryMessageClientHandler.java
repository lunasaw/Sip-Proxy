package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackListQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientQueryEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §9.5 / A.2.4.11 巡航轨迹列表查询请求 (cmdType=CruiseTrackListQuery)。
 *
 * <p>v1.5.0 改造：只发布 {@link ClientQueryEvent}，回包由 {@code ClientListenerAdapter}
 * 路由到 {@code QueryListener.onCruiseTrackListQuery}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class CruiseTrackListQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.CRUISE_TRACK_LIST_QUERY.getType();
    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return QUERY;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            CruiseTrackListQuery query = parseXml(CruiseTrackListQuery.class);
            if (query == null) {
                log.warn("CruiseTrackList 查询解析失败");
                return;
            }
            publisher.publishEvent(new ClientQueryEvent(this,
                    deviceSession.getUserId(), deviceSession.getSipId(), query));
        } catch (Exception e) {
            log.error("处理巡航轨迹列表查询请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
