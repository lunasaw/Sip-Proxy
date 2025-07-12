package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import javax.sip.RequestEvent;

import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientSendCmd;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageProcessorClient;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备目录查询消息处理器
 * 负责处理设备目录查询请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class CatalogQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";

    private String cmdType = CMD_TYPE;

    public CatalogQueryMessageClientHandler(MessageProcessorClient messageProcessorClient) {
        super(messageProcessorClient);
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

            log.debug("处理设备目录查询: userId={}, sipId={}", userId, sipId);

            // 解析查询请求
            DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
            String sn = deviceQuery.getSn();

            // 调用业务处理器获取设备目录信息
            DeviceResponse deviceResponse = messageProcessorClient.getDeviceItem(userId);
            deviceResponse.setSn(sn);

            // 发送响应
            ClientSendCmd.deviceChannelCatalogResponse(userId, sipId, deviceResponse);

        } catch (Exception e) {
            log.error("处理设备目录查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
