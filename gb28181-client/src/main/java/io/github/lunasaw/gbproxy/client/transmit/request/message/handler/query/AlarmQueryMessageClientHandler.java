package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;

import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备告警查询消息处理器
 * 负责处理设备告警查询请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class AlarmQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Alarm";

    private String cmdType = CMD_TYPE;

    public AlarmQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
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
            String userId = deviceSession.getUserId();
            String sipId = deviceSession.getSipId();

            log.debug("处理设备告警查询: userId={}, sipId={}", userId, sipId);

            // 解析告警查询请求
            DeviceAlarmQuery deviceAlarmQuery = parseXml(DeviceAlarmQuery.class);
            String sn = deviceAlarmQuery.getSn();

            // 调用业务处理器获取设备告警信息
            DeviceAlarmNotify deviceAlarmNotify = messageRequestHandler.getDeviceAlarmNotify(deviceAlarmQuery);
            deviceAlarmNotify.setSn(sn);

            // 发送响应
            ClientCommandSender.sendAlarmCommand(deviceSession.getFromDevice(), deviceSession.getToDevice(), deviceAlarmNotify);

        } catch (Exception e) {
            log.error("处理设备告警查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
