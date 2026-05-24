package io.github.lunasaw.gbproxy.server.transmit.request.message.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceSdCardStatusEvent;
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
 * GB28181-2022 §9.5 / A.2.6.16 存储卡状态查询应答 处理器
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class SdCardStatusMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.SD_CARD_STATUS.getType();

    private String cmdType = CMD_TYPE;

    public SdCardStatusMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        SDCardStatusResponse response = parseXml(SDCardStatusResponse.class);
        if (response == null) {
            log.warn("解析存储卡状态查询应答失败: deviceId={}", userId);
            return;
        }
        publisher.publishEvent(new DeviceSdCardStatusEvent(this, userId, response));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
