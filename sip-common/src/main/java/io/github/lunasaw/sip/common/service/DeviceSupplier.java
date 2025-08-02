package io.github.lunasaw.sip.common.service;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.ToDevice;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 设备提供器接口
 * 用于动态获取设备列表的hook机制，支持外部实现自定义的设备获取逻辑
 * <p>
 * 设计原则：
 * 1. 业务方通过userId获取设备数据，项目本身不关心设备类型
 * 2. 简化接口设计，减少不必要的复杂性
 * 3. 支持动态设备管理和更新
 *
 * @author luna
 * @date 2025/01/23
 */
public interface DeviceSupplier {

    /**
     * 获取当前可用的设备列表
     * 该方法会被定期调用以获取最新的设备信息
     *
     * @return 设备列表，如果没有可用设备则返回空列表
     */
    List<Device> getDevices();

    /**
     * 根据用户ID获取指定设备
     * 这是设备获取的核心方法，业务方通过userId获取设备数据
     *
     * @param userId 用户ID
     * @return 设备信息，如果不存在则返回null
     */
    Device getDevice(String userId);

    /**
     * 添加或更新设备
     * 根据设备中的userId进行添加或更新操作
     *
     * @param device 设备信息
     */
    void addOrUpdateDevice(Device device);

    /**
     * 移除设备
     *
     * @param userId 用户ID
     */
    void removeDevice(String userId);

    /**
     * 获取设备数量
     *
     * @return 设备数量
     */
    int getDeviceCount();

    /**
     * 获取设备提供器的名称标识
     *
     * @return 提供器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    default ToDevice getToDevice(String deviceId) {
        Assert.notNull(deviceId, "设备Id不能为空");
        Device device = getDevice(deviceId);
        Assert.notNull(device, "查询不到设备信息");
        return getToDevice(device);
    }

    default ToDevice getToDevice(Device device) {
        if (device == null) {
            return null;
        }
        ToDevice toDevice = new ToDevice();
        toDevice.setHostAddress(device.getHostAddress());
        toDevice.setUserId(device.getUserId());
        toDevice.setRealm(device.getRealm());
        toDevice.setTransport(device.getTransport());
        toDevice.setStreamMode(device.getStreamMode());
        toDevice.setIp(device.getIp());
        toDevice.setPort(device.getPort());
        toDevice.setPassword(device.getPassword());
        toDevice.setCharset(device.getCharset());
        return toDevice;
    }
}