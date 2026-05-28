package io.github.lunasaw.gbproxy.server.transmit.request.message.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §A.2.6.10 设备预置位查询应答 处理器
 * (rootType=Response, cmdType=PresetQuery, method=MESSAGE)。
 *
 * <p>v1.5.6 起补全：解析 {@link PresetQueryResponse} 并发布
 * {@link ServerQueryResponseEvent}，由 {@code ServerListenerAdapter} 路由到
 * {@code DeviceResponseListener.onPresetQueryResponse}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class PresetQueryResponseMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.PRESET_QUERY.getType();

    private String cmdType = CMD_TYPE;

    public PresetQueryResponseMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        super(publisher, serverDeviceSupplier);
    }

    @Override
    public String getRootType() {
        return RESPONSE;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        if (!preCheck(event)) {
            return;
        }
        DeviceSession deviceSession = getDeviceSession(event);
        String userId = deviceSession.getUserId();

        PresetQueryResponse response = parseXml(PresetQueryResponse.class);
        if (response == null) {
            log.warn("解析设备预置位查询应答失败: deviceId={}", userId);
            return;
        }

        publisher.publishEvent(new ServerQueryResponseEvent(this, userId, response.getSn(), response));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
