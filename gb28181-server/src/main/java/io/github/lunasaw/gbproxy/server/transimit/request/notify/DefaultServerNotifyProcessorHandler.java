package io.github.lunasaw.gbproxy.server.transimit.request.notify;

import io.github.lunasaw.sip.common.entity.FromDevice;
import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * Server模块NOTIFY请求处理器业务接口默认实现
 *
 * @author luna
 */
@Slf4j
public class DefaultServerNotifyProcessorHandler implements ServerNotifyProcessorHandler {

    @Override
    public void handleNotifyRequest(RequestEvent evt, FromDevice fromDevice) {
        log.debug("默认处理NOTIFY请求：事件 = {}, 发送设备 = {}", evt, fromDevice);
    }

    @Override
    public boolean validateDevicePermission(RequestEvent evt) {
        log.debug("默认验证设备权限：事件 = {}", evt);
        return true;
    }

    @Override
    public FromDevice getFromDevice() {
        log.debug("默认获取发送设备信息");
        return null;
    }

    @Override
    public void handleNotifyError(RequestEvent evt, String errorMessage) {
        log.debug("默认处理NOTIFY请求错误：事件 = {}, 错误消息 = {}", evt, errorMessage);
    }
}