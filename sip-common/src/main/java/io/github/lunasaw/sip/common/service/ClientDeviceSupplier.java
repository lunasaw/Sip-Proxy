package io.github.lunasaw.sip.common.service;

import io.github.lunasaw.sip.common.entity.FromDevice;

/**
 * 客户端设备提供器接口
 * 扩展DeviceSupplier接口，提供客户端特定的设备获取能力
 * <p>
 * 设计原则：
 * 1. 继承基础设备提供器接口，保持接口的一致性
 * 2. 提供客户端发送方设备信息获取能力
 * 3. 支持客户端SIP消息发送时的设备标识
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
}