package io.github.lunasaw.gbproxy.server.transimit.request.message.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gbproxy.server.transimit.request.message.MessageServerHandlerAbstract;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageRequestProcessor;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 复制类 无实际使用
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceInfoMessageServerHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.DEVICE_INFO.getType();

    private String cmdType = CMD_TYPE;

    public DeviceInfoMessageServerHandler(ServerMessageProcessorHandler serverMessageProcessorHandler, ServerDeviceSupplier serverDeviceSupplier) {
        super(serverMessageProcessorHandler, serverDeviceSupplier);
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

        DeviceInfo deviceInfo = parseXml(DeviceInfo.class);


        serverMessageProcessorHandler.updateDeviceInfo(userId, deviceInfo);
    }


    @Override
    public String getCmdType() {
        return cmdType;
    }


}
