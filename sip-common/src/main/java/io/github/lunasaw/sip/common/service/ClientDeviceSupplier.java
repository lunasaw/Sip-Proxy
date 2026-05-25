package io.github.lunasaw.sip.common.service;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.utils.SipUtils;

import javax.sip.RequestEvent;

/**
 * 客户端设备提供器接口
 * 扩展DeviceSupplier接口，提供客户端特定的设备获取能力
 *
 * @author luna
 * @date 2025/01/23
 */
public interface ClientDeviceSupplier extends DeviceSupplier {

    /**
     * 获取客户端发送方设备信息
     * 用于客户端发送SIP消息时标识发送方设备
     *
     * @return 客户端发送方设备信息，如果不存在则返回null
     */
    FromDevice getClientFromDevice();

    /**
     * 设置客户端发送方设备信息
     * 用于配置客户端的发送方设备标识
     *
     * @param fromDevice 客户端发送方设备信息
     */
    void setClientFromDevice(FromDevice fromDevice);

    /**
     * 判断收到的请求是否发往本端客户端身份。
     *
     * <p>用于同 JVM 同时启用 client/server（比如集成测试）时，
     * 让两侧处理器只认领与自己 To-Header userId 匹配的请求，避免重复处理。
     *
     * @param evt SIP 请求事件
     * @return true 收件人是本客户端
     */
    default boolean checkDevice(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        String userId = SipUtils.getUserIdFromToHeader(request);
        FromDevice fromDevice = getClientFromDevice();
        if (fromDevice == null) {
            return false;
        }
        return userId.equals(fromDevice.getUserId());
    }
}
