package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 设备状态查询消息处理器
 * 负责处理设备状态查询请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceStatusQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "DeviceStatus";

    private String cmdType = CMD_TYPE;

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    public DeviceStatusQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
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

            log.info("处理设备状态查询: userId={}, sipId={}", userId, sipId);

            // 解析查询请求
            DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
            String sn = deviceQuery.getSn();

            // 调用业务处理器获取设备状态
            DeviceStatus deviceStatus = messageRequestHandler.getDeviceStatus(userId);
            deviceStatus.setSn(sn);

            // 发送响应
            FromDevice clientFromDevice = deviceSession.getFromDevice();
            if (clientFromDevice == null) {
                log.warn("客户端设备信息未找到，无法发送设备状态响应: userId={}, sipId={}", userId, sipId);
                return;
            }

            ClientCommandSender.sendDeviceStatusCommand(deviceSession.getFromDevice(), deviceSession.getToDevice(), deviceStatus);
        } catch (Exception e) {
            log.error("处理设备状态查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
