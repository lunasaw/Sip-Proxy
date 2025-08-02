package io.github.lunasaw.gbproxy.server.transmit.request.notify;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.sip.common.entity.FromDevice;

import javax.sip.RequestEvent;

/**
 * Server模块NOTIFY请求处理器业务接口
 * 负责具体的NOTIFY请求业务逻辑实现
 *
 * @author luna
 */
public interface ServerNotifyProcessorHandler {

    /**
     * 处理NOTIFY请求
     *
     * @param evt        请求事件
     * @param fromDevice 发送设备
     */
    default void handleNotifyRequest(RequestEvent evt, FromDevice fromDevice) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 验证设备权限
     *
     * @param evt 请求事件
     * @return 是否有权限
     */
    default boolean validateDevicePermission(RequestEvent evt) {
        return true; // 默认验证通过
    }


    /**
     * 处理NOTIFY请求错误
     *
     * @param evt          请求事件
     * @param errorMessage 错误消息
     */
    default void handleNotifyError(RequestEvent evt, String errorMessage) {
        // 默认实现为空，由业务方根据需要实现
    }


    default void deviceNotifyUpdate(String userId, DeviceOtherUpdateNotify deviceOtherUpdateNotify) {
        // 业务处理逻辑
    }
}