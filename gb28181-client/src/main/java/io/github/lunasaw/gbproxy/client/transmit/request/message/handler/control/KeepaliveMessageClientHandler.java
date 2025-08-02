package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import io.github.lunasaw.gb28181.common.entity.control.KeepaliveControl;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * Keepalive消息客户端处理器
 * 负责处理Control类型的Keepalive命令
 *
 * @author claude
 * @date 2025/01/19
 */
@Component
@Slf4j
@Getter
@Setter
public class KeepaliveMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Keepalive";
    private String cmdType = CMD_TYPE;

    public KeepaliveMessageClientHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public String getRootType() {
        return "Control";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            String userId = deviceSession.getUserId();
            String sipId = deviceSession.getSipId();

            log.debug("处理Keepalive心跳请求: userId={}, sipId={}", userId, sipId);

            // 解析心跳请求
            KeepaliveControl keepalive = parseXml(KeepaliveControl.class);
            String sn = keepalive.getSn();

            log.debug("收到Keepalive心跳: userId={}, sipId={}, sn={}, status={}", 
                     userId, sipId, sn, keepalive.getStatus());

            // 调用业务处理器处理心跳
            if (messageRequestHandler != null) {
                // 这里可以根据需要调用业务处理方法
                log.info("处理Keepalive心跳成功: deviceId={}, status={}", keepalive.getDeviceId(), keepalive.getStatus());
            }

        } catch (Exception e) {
            log.error("处理Keepalive心跳请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}