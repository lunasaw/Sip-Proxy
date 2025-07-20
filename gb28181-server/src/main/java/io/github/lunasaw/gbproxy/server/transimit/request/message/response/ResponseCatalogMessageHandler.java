package io.github.lunasaw.gbproxy.server.transimit.request.message.response;

import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
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
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class ResponseCatalogMessageHandler extends MessageServerHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";

    public ResponseCatalogMessageHandler(ServerMessageProcessorHandler serverMessageProcessorHandler, ServerDeviceSupplier serverDeviceSupplier) {
        super(serverMessageProcessorHandler, serverDeviceSupplier);
    }


    @Override
    public void handForEvt(RequestEvent event) {
        if (!preCheck(event)) {
            return;
        }
        DeviceSession deviceSession = getDeviceSession(event);
        String userId = deviceSession.getUserId();
        DeviceResponse deviceResponse = parseXml(DeviceResponse.class);

        serverMessageProcessorHandler.updateDeviceResponse(userId, deviceResponse);
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }

    @Override
    public String getRootType() {
        return RESPONSE;
    }
}
