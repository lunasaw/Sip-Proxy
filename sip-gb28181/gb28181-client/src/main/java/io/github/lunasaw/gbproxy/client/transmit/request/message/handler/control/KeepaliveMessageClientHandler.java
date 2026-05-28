package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import io.github.lunasaw.gb28181.common.entity.control.KeepaliveControl;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientKeepaliveEvent;
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
 * Keepalive 消息客户端处理器（cmdType=Keepalive，rootType=Control）。
 *
 * <p>v1.5.0 改造：发布 {@link ClientKeepaliveEvent}，由 Adapter 派发到 {@code ControlListener.onKeepalive}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class KeepaliveMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Keepalive";
    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return "Control";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            KeepaliveControl keepalive = parseXml(KeepaliveControl.class);
            if (keepalive == null) {
                log.warn("Keepalive 心跳解析失败");
                return;
            }
            log.debug("收到 Keepalive 心跳: userId={}, sipId={}, sn={}, status={}",
                    deviceSession.getUserId(), deviceSession.getSipId(),
                    keepalive.getSn(), keepalive.getStatus());
            publisher.publishEvent(new ClientKeepaliveEvent(this, deviceSession.getUserId(), keepalive));
        } catch (Exception e) {
            log.error("处理 Keepalive 心跳请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
