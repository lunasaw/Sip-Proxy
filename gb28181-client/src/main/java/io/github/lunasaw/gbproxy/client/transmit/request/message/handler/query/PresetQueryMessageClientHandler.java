package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.query;

import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
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
 * 设备预置位查询消息处理器
 * 负责处理设备预置位查询请求
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class PresetQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "PresetQuery";

    private String cmdType = CMD_TYPE;

    public PresetQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
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

            log.debug("处理设备预置位查询: userId={}, sipId={}", userId, sipId);

            // 解析查询请求
            PresetQuery presetQuery = parseXml(PresetQuery.class);
            String sn = presetQuery.getSn();

            // 调用业务处理器获取设备预置位信息（需在MessageRequestHandler中扩展方法）
            PresetQueryResponse presetQueryResponse = messageRequestHandler.getPresetQueryResponse(userId);
            presetQueryResponse.setSn(sn);

            // 发送响应
            ClientCommandSender.sendPresetQueryResponse(deviceSession.getFromDevice(), deviceSession.getToDevice(), presetQueryResponse);
        } catch (Exception e) {
            log.error("处理设备预置位查询时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}