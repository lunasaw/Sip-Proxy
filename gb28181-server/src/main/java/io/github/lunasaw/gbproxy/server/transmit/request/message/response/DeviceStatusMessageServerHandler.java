package io.github.lunasaw.gbproxy.server.transmit.request.message.response;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
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
 * 设备状态消息处理器
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceStatusMessageServerHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.DEVICE_STATUS.getType();

    private String cmdType = CMD_TYPE;

    public DeviceStatusMessageServerHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
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

        DeviceStatus deviceStatus = parseXml(DeviceStatus.class);


        publisher.publishEvent(new ServerQueryResponseEvent(this, userId, deviceStatus.getSn(), deviceStatus));
    }


    @Override
    public String getCmdType() {
        return cmdType;
    }


}
