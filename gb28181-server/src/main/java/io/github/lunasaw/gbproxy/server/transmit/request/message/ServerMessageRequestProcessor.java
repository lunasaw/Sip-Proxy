package io.github.lunasaw.gbproxy.server.transmit.request.message;

import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
 * 只负责SIP协议层面的处理，业务逻辑通过ServerMessageProcessorHandler接口实现
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
    @Lazy
    private ServerMessageProcessorHandler serverMessageProcessorHandler;

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Override
    public void process(RequestEvent evt) {
        process(evt, null);
    }

    @Override
    public void process(RequestEvent evt, ServerTransaction serverTransaction) {
        try {
            // 验证设备权限
            if (!serverMessageProcessorHandler.validateDevicePermission(evt)) {
                log.warn("MESSAGE请求权限验证失败");
                serverMessageProcessorHandler.handleMessageError(evt, "权限验证失败");
                return;
            }

            // 获取发送设备信息
            FromDevice fromDevice = serverDeviceSupplier.getServerFromDevice();
            if (fromDevice == null) {
                log.warn("MESSAGE请求无法获取发送设备信息");
                serverMessageProcessorHandler.handleMessageError(evt, "无法获取发送设备信息");
                return;
            }

            // 处理MESSAGE请求，传入预创建的事务
            doMessageHandForEvt(evt, fromDevice, serverTransaction);

        } catch (Exception e) {
            log.error("处理MESSAGE请求异常：evt = {}", evt, e);
            serverMessageProcessorHandler.handleMessageError(evt, e.getMessage());
        }
    }

}
