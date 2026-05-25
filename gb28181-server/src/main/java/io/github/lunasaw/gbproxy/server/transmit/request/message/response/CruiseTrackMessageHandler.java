package io.github.lunasaw.gbproxy.server.transmit.request.message.response;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gbproxy.server.transmit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §9.5 / A.2.6.14 巡航轨迹查询应答 处理器
 *
 * @author luna
 */
@Component
@Slf4j
public class CruiseTrackMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.CRUISE_TRACK_QUERY.getType();

    public CruiseTrackMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        CruiseTrackResponse response = parseXml(CruiseTrackResponse.class);
        if (response == null) {
            log.warn("解析巡航轨迹查询应答失败: deviceId={}", userId);
            return;
        }
        publisher.publishEvent(new ServerQueryResponseEvent(this, userId, null, response));
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }
}
