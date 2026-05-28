package io.github.lunasaw.gbproxy.server.transmit.request.message;

import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.transmit.event.message.SipMessageRequestProcessorAbstract;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Server模块MESSAGE请求处理器
 *
 * @author luna
 */
@Component("serverMessageRequestProcessor")
@Getter
@Setter
@Slf4j
public class ServerMessageRequestProcessor extends SipMessageRequestProcessorAbstract {

    public static final String METHOD = "MESSAGE";

    private String method = METHOD;

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Override
    public void process(RequestEvent evt) {
        process(evt, null);
    }

    @Override
    public void process(RequestEvent evt, ServerTransaction serverTransaction) {
        try {
            if (!serverDeviceSupplier.checkDevice(evt)) {
                log.warn("MESSAGE请求权限验证失败");
                return;
            }

            FromDevice fromDevice = serverDeviceSupplier.getServerFromDevice();
            if (fromDevice == null) {
                log.warn("MESSAGE请求无法获取发送设备信息");
                return;
            }

            doMessageHandForEvt(evt, fromDevice, serverTransaction);

        } catch (Exception e) {
            log.error("处理MESSAGE请求异常：evt = {}", evt, e);
        }
    }

}
