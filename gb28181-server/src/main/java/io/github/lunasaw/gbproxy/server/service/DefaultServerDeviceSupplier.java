package io.github.lunasaw.gbproxy.server.service;

import io.github.lunasaw.gbproxy.server.config.ServerProperties;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端设备提供器默认实现
 * 基于Gb28181ServerProperties配置的服务端设备管理
 * <p>
 * 设计原则：
 * 1. 线程安全的设备管理
 * 2. 基于配置的服务端设备初始化
 * 3. 支持动态设备添加和移除
 * 4. 自动生成服务端发送方设备信息
 *
 * @author luna
 * @date 2025/01/23
 */
@Slf4j
@Service
public class DefaultServerDeviceSupplier implements ServerDeviceSupplier {

    /**
     * 设备存储容器 - 线程安全
     */
    private final ConcurrentHashMap<String, Device> deviceMap = new ConcurrentHashMap<>();

    /**
     * 服务端发送方设备信息
     */
    private FromDevice serverFromDevice;

    /**
     * GB28181服务端配置属性
     */
    @Autowired
    private ServerProperties serverProperties;

    /**
     * 初始化服务端发送方设备信息
     */
    public DefaultServerDeviceSupplier() {
        // 构造函数中不进行初始化，等待配置注入后再初始化
    }

    /**
     * 初始化服务端发送方设备信息
     * 基于配置属性创建服务端设备
     */
    public void initializeServerFromDevice() {
        if (serverProperties != null) {
            this.serverFromDevice = FromDevice.getInstance(
                    serverProperties.getServerId(),
                    serverProperties.getIp(),
                    serverProperties.getPort()
            );
            log.info("服务端发送方设备初始化完成: {}", serverFromDevice.getUserId());
        } else {
            log.warn("Gb28181ServerProperties未注入，使用默认服务端设备配置");
            this.serverFromDevice = FromDevice.getInstance(
                    "34020000002000000001",
                    "0.0.0.0",
                    5060
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
    public FromDevice getServerFromDevice() {
        // 延迟初始化，确保配置已注入
        if (serverFromDevice == null) {
            initializeServerFromDevice();
        }
        return serverFromDevice;
    }

    @Override
    public void setServerFromDevice(FromDevice fromDevice) {
        this.serverFromDevice = fromDevice;
        log.info("服务端发送方设备设置成功: {}", fromDevice != null ? fromDevice.getUserId() : "null");
    }

    @Override
    public String getName() {
        return "DefaultServerDeviceSupplier";
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