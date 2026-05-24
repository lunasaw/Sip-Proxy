package io.github.lunasaw.gbproxy.server.transmit.request.message.response;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
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
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class RecordInfoMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = "RecordInfo";

    private String cmdType = CMD_TYPE;

    public RecordInfoMessageHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        DeviceRecord deviceRecord = parseXml(DeviceRecord.class);
        publisher.publishEvent(new ServerQueryResponseEvent(this, userId, deviceRecord.getSn(), deviceRecord));
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
