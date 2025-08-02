package io.github.lunasaw.gbproxy.client.service;

import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端设备提供器默认实现
 * 基于SipClientProperties配置的客户端设备管理
 * <p>
 * 设计原则：
 * 1. 线程安全的设备管理
 * 2. 基于配置的客户端设备初始化
 * 3. 支持动态设备添加和移除
 * 4. 自动生成客户端发送方设备信息
 *
 * @author luna
 * @date 2025/8/2
 */
@Slf4j
@Service
@ConditionalOnMissingBean(ClientDeviceSupplier.class)
public class DefaultClientDeviceSupplier implements ClientDeviceSupplier {

    /**
     * 设备存储容器 - 线程安全
     */
    private final ConcurrentHashMap<String, Device> deviceMap = new ConcurrentHashMap<>();

    /**
     * 客户端发送方设备信息
     */
    private FromDevice clientFromDevice;

    /**
     * GB28181客户端配置属性
     */
    @Autowired
    private SipClientProperties clientProperties;

    /**
     * 初始化客户端发送方设备信息
     */
    public DefaultClientDeviceSupplier() {
        // 构造函数中不进行初始化，等待配置注入后再初始化
    }

    /**
     * 初始化客户端发送方设备信息
     * 基于配置属性创建客户端设备
     */
    public void initializeClientFromDevice() {
        if (clientProperties != null) {
            this.clientFromDevice = FromDevice.getInstance(
                    clientProperties.getClientId(),
                    clientProperties.getDomain(),
                    clientProperties.getPort()
            );
            log.info("客户端发送方设备初始化完成: {}", clientFromDevice.getUserId());
        } else {
            log.warn("SipClientProperties未注入，使用默认客户端设备配置");
            this.clientFromDevice = FromDevice.getInstance(
                    "34020000001320000001",
                    "127.0.0.1",
                    5061
            );
        }
    }

    @Override
    public Device getDevice(String userId) {
        if (userId == null) {
            log.warn("获取设备时userId为空");
            return null;
        }
        Device device = deviceMap.get(userId);
        if (device == null) {
            log.debug("未找到设备: {}", userId);
        }
        return device;
    }

    @Override
    public FromDevice getClientFromDevice() {
        // 延迟初始化，确保配置已注入
        if (clientFromDevice == null) {
            initializeClientFromDevice();
        }
        return clientFromDevice;
    }

    @Override
    public void setClientFromDevice(FromDevice fromDevice) {
        this.clientFromDevice = fromDevice;
        log.info("客户端发送方设备设置成功: {}", fromDevice != null ? fromDevice.getUserId() : "null");
    }

    @Override
    public String getName() {
        return "DefaultClientDeviceSupplier";
    }

    /**
     * 清空所有设备
     */
    public void clearAllDevices() {
        int count = deviceMap.size();
        deviceMap.clear();
        log.info("清空所有设备，共移除 {} 个设备", count);
    }

    /**
     * 检查设备是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean containsDevice(String userId) {
        return userId != null && deviceMap.containsKey(userId);
    }
}
