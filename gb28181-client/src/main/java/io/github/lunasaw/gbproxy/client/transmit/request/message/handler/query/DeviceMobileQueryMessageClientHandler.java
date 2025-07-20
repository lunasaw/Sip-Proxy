package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 设备移动位置查询消息处理器
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceMobileQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "MobilePosition";

    private String cmdType = CMD_TYPE;

    public DeviceMobileQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public String getRootType() {
        return "Query";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            MobilePositionQuery deviceMobileQuery = parseXml(MobilePositionQuery.class);

            // 直接调用业务方法获取完整应答
            MobilePositionNotify mobilePositionNotify = messageRequestHandler.getMobilePositionNotify(deviceMobileQuery);

            // 发送应答
            ClientCommandSender.sendMobilePositionNotify(deviceSession.getFromDevice(), deviceSession.getToDevice(), mobilePositionNotify);
        } catch (Exception e) {
            log.error("处理设备预置位查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
