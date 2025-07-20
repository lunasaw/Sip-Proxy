package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.notify;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;

import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 广播通知消息处理器
 * 负责处理广播通知请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class BroadcastNotifyMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Broadcast";

    private String cmdType = CMD_TYPE;

    public BroadcastNotifyMessageHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public String getRootType() {
        return "Notify";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            String userId = deviceSession.getUserId();
            String sipId = deviceSession.getSipId();

            log.debug("处理广播通知: userId={}, sipId={}", userId, sipId);

            // 解析广播通知
            DeviceBroadcastNotify broadcastNotify = parseXml(DeviceBroadcastNotify.class);

            // 调用业务处理器处理广播通知
            messageRequestHandler.broadcastNotify(broadcastNotify);

        } catch (Exception e) {
            log.error("处理广播通知时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
