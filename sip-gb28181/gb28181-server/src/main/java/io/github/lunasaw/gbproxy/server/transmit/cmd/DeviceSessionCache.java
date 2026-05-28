package io.github.lunasaw.gbproxy.server.transmit.cmd;

import io.github.lunasaw.sip.common.entity.ToDevice;

/**
 * 设备会话缓存接口，由业务方实现。
 * 框架通过此接口获取设备注册时缓存的寻址信息（ip/port/transport），
 * 使 ServerCommandSender 的调用方只需传 deviceId。
 */
public interface DeviceSessionCache {

    /**
     * 根据设备 ID 获取寻址信息。
     * 实现方只需填充 userId/ip/port/transport，其余字段由框架按需设置。
     *
     * @param deviceId GB28181 设备���号
     * @return ToDevice，若设备未注册返回 null
     */
    ToDevice getToDevice(String deviceId);
}
