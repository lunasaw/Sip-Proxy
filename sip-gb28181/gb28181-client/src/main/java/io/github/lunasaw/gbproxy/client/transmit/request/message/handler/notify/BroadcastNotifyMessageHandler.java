package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.notify;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientNotifyEvent;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 广播通知消息处理器（cmdType=Broadcast，rootType=Notify）。
 *
 * <p>v1.5.0 改造：只发布 {@link ClientNotifyEvent}，回包由 {@code ClientListenerAdapter}
 * 路由到 {@code NotifyListener.onBroadcastNotify}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class BroadcastNotifyMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Broadcast";

    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return "Notify";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            DeviceBroadcastNotify broadcastNotify = parseXml(DeviceBroadcastNotify.class);
            if (broadcastNotify == null) {
                log.warn("Broadcast 通知解析失败");
                return;
            }
            publisher.publishEvent(new ClientNotifyEvent(this, deviceSession.getUserId(), broadcastNotify));
        } catch (Exception e) {
            log.error("处理广播通知时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
