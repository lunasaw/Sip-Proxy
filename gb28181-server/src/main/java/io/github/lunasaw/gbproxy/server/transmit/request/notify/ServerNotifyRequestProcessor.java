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
 * 只负责SIP协议层面的处理，业务逻辑通过ServerNotifyProcessorHandler接口实现
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
    private ServerNotifyProcessorHandler serverNotifyProcessorHandler;

    @Autowired
    private ServerDeviceSupplier deviceSupplier;

    /**
     * 处理NOTIFY请求
     * 只负责SIP协议层面的处理，业务逻辑通过ServerNotifyProcessorHandler接口实现
     *
     * @param evt 请求事件
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            log.debug("处理NOTIFY请求：evt = {}", evt);

            // 验证设备权限
            if (!serverNotifyProcessorHandler.validateDevicePermission(evt)) {
                log.warn("NOTIFY请求权限验证失败");
                serverNotifyProcessorHandler.handleNotifyError(evt, "权限验证失败");
                return;
            }

            // 获取发送设备信息
            FromDevice fromDevice = deviceSupplier.getServerFromDevice();
            if (fromDevice == null) {
                log.warn("NOTIFY请求无法获取发送设备信息");
                serverNotifyProcessorHandler.handleNotifyError(evt, "无法获取发送设备信息");
                return;
            }

            // 处理NOTIFY请求
            doMessageHandForEvt(evt, fromDevice);

        } catch (Exception e) {
            log.error("处理NOTIFY请求异常：evt = {}", evt, e);
            serverNotifyProcessorHandler.handleNotifyError(evt, e.getMessage());
        }
    }

}
