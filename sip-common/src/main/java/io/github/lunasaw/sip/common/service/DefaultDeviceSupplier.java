package io.github.lunasaw.sip.common.service;

import io.github.lunasaw.sip.common.entity.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认设备提供器实现
 * 基于内存存储管理设备信息，支持动态添加、更新和删除设备
 *
 * 设计原则：
 * 1. 业务方通过userId获取设备数据，项目本身不关心设备类型
 * 2. 简化存储结构，只维护userId到设备的映射
 * 3. 支持动态设备管理和更新
 *
 * @author luna
 * @date 2025/01/23
 */
@Slf4j
@Component
public class DefaultDeviceSupplier implements DeviceSupplier {

    /**
     * 用户ID到设备的映射
     */
    private final Map<String, Device> userIdMap = new ConcurrentHashMap<>();

    @Override
    public List<Device> getDevices() {
        return userIdMap.values().stream()
            .collect(Collectors.toList());
    }

    @Override
    public Device getDevice(String userId) {
        Device device = userIdMap.get(userId);
        if (device == null) {
            log.warn("未找到用户ID为 {} 的设备", userId);
        }
        return device;
    }

    @Override
    public void addOrUpdateDevice(Device device) {
        if (device == null) {
            log.warn("尝试添加空设备");
            return;
        }

        if (device.getUserId() == null || device.getUserId().trim().isEmpty()) {
            log.warn("设备用户ID不能为空");
            return;
        }

        // 更新用户ID映射
        userIdMap.put(device.getUserId(), device);

        log.info("设备添加/更新成功 - 用户ID: {}, IP: {}:{}",
            device.getUserId(), device.getIp(), device.getPort());
    }

    @Override
    public void removeDevice(String userId) {
        Device removedDevice = userIdMap.remove(userId);
        if (removedDevice != null) {
            log.info("设备移除成功 - 用户ID: {}", userId);
        } else {
            log.warn("未找到要移除的设备，用户ID: {}", userId);
        }
    }

    @Override
    public int getDeviceCount() {
        return userIdMap.size();
    }

    /**
     * 批量添加设备
     *
     * @param devices 设备列表
     */
    public void addDevices(List<Device> devices) {
        if (devices != null) {
            devices.forEach(this::addOrUpdateDevice);
        }
    }

    /**
     * 清空所有设备
     */
    public void clear() {
        userIdMap.clear();
        log.info("所有设备已清空");
    }

    /**
     * 检查设备是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean hasDevice(String userId) {
        return userIdMap.containsKey(userId);
    }
}