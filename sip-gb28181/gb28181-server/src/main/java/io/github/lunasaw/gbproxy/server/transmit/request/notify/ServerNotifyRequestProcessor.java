package io.github.lunasaw.gbproxy.server.transmit.request.notify;

import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.transmit.event.message.SipMessageRequestProcessorAbstract;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Server模块NOTIFY请求处理器
 *
 * @author luna
 */
@Component("serverNotifyRequestProcessor")
@Getter
@Setter
@Slf4j
public class ServerNotifyRequestProcessor extends SipMessageRequestProcessorAbstract {

    public static final String METHOD = "NOTIFY";

    private String method = METHOD;

    @Autowired
    private ServerDeviceSupplier deviceSupplier;

    @Override
    public void process(RequestEvent evt) {
        try {
            log.debug("处理NOTIFY请求：evt = {}", evt);

            if (!deviceSupplier.checkDevice(evt)) {
                log.warn("NOTIFY请求权限验证失败");
                return;
            }

            FromDevice fromDevice = deviceSupplier.getServerFromDevice();
            if (fromDevice == null) {
                log.warn("NOTIFY请求无法获取发送设备信息");
                return;
            }

            doMessageHandForEvt(evt, fromDevice);

        } catch (Exception e) {
            log.error("处理NOTIFY请求异常：evt = {}", evt, e);
        }
    }

}
