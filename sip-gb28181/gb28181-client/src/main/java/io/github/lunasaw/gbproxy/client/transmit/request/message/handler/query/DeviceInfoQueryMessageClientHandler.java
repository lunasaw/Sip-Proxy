package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import javax.sip.RequestEvent;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientQueryEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 设备信息查询消息处理器（cmdType=DeviceInfo，rootType=Query）。
 *
 * <p>v1.5.0 改造：只发布 {@link ClientQueryEvent}，回包由 {@code ClientListenerAdapter}
 * 路由到 {@code QueryListener.onDeviceInfoQuery}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class DeviceInfoQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "DeviceInfo";
    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return "Query";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
            if (deviceQuery == null) {
                log.warn("DeviceInfo 查询解析失败");
                return;
            }
            publisher.publishEvent(new ClientQueryEvent(this,
                    deviceSession.getUserId(), deviceSession.getSipId(), deviceQuery));
        } catch (Exception e) {
            log.error("处理设备信息查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
