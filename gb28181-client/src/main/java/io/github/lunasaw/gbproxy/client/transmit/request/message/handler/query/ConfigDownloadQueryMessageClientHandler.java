package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.query.ConfigDownloadQuery;
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
 * 设备配置查询消息处理器（cmdType=ConfigDownload，rootType=Query；payload=ConfigDownloadQuery）。
 *
 * <p>v1.5.0 改造：只发布 {@link ClientQueryEvent}，回包由 {@code ClientListenerAdapter}
 * 路由到 {@code QueryListener.onConfigDownloadQueryV2}。
 *
 * <p>注意：本 handler 与 {@link ConfigDownloadMessageHandler} 共用同一 cmdType
 * （历史并行结构，dispatch map 由后注册的胜出，是 pre-existing 现象，本次重构保留）。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ConfigDownloadQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "ConfigDownload";
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
            ConfigDownloadQuery query = parseXml(ConfigDownloadQuery.class);
            if (query == null) {
                log.warn("ConfigDownloadQuery 查询解析失败");
                return;
            }
            publisher.publishEvent(new ClientQueryEvent(this,
                    deviceSession.getUserId(), deviceSession.getSipId(), query));
        } catch (Exception e) {
            log.error("处理设备配置查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
