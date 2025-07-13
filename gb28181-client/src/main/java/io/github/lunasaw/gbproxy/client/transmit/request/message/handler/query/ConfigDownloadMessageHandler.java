package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 设备配置下载消息处理器
 * 负责处理设备配置下载请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class ConfigDownloadMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "ConfigDownload";

    private String cmdType = CMD_TYPE;

    public ConfigDownloadMessageHandler(MessageRequestHandler messageRequestHandler) {
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

            log.debug("处理设备配置下载: userId={}, sipId={}", userId, sipId);

            // 解析配置下载请求
            DeviceConfigDownload deviceConfigDownload = parseXml(DeviceConfigDownload.class);

            // 调用业务处理器获取设备配置
            DeviceConfigResponse deviceConfigResponse = messageRequestHandler.getDeviceConfigResponse(deviceConfigDownload);
            deviceConfigResponse.setSn(deviceConfigDownload.getSn());

            // 发送响应
            ClientCommandSender.sendDeviceConfigCommand(deviceSession.getFromDevice(), deviceSession.getToDevice(), deviceConfigResponse);

        } catch (Exception e) {
            log.error("处理设备配置下载时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
