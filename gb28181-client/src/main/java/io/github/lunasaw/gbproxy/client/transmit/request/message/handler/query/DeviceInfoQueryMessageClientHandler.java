package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备信息查询消息处理器
 * 负责处理设备信息查询请求
 *
 * @author weidian
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceInfoQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "DeviceInfo";

    private String cmdType = CMD_TYPE;

    public DeviceInfoQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public String getRootType() {
        return ClientMessageRequestProcessor.METHOD + "Query";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            DeviceSession deviceSession = getDeviceSession(event);
            String userId = deviceSession.getUserId();
            String sipId = deviceSession.getSipId();

            log.debug("处理设备信息查询: userId={}, sipId={}", userId, sipId);

            // 解析查询请求
            DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
            String sn = deviceQuery.getSn();

            // 调用业务处理器获取设备信息
            DeviceInfo deviceInfo = messageRequestHandler.getDeviceInfo(userId);
            deviceInfo.setSn(sn);

            // 发送响应
            ClientCommandSender.sendDeviceInfoCommand(deviceSession.getFromDevice(), deviceSession.getToDevice(), deviceInfo);

        } catch (Exception e) {
            log.error("处理设备信息查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
