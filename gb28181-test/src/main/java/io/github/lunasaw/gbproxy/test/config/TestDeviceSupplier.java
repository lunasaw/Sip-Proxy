package io.github.lunasaw.gbproxy.test.config;

import com.google.common.collect.Lists;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 测试设备提供器实现
 * 专门用于测试环境，提供预配置的设备
 *
 * 设计原则：
 * 1. 业务方通过userId获取设备数据，项目本身不关心设备类型
 * 2. 统一存储Device对象，避免数据覆盖问题
 * 3. 提供转换方法，在使用时根据场景转换为FromDevice或ToDevice
 * 4. 支持动态设备管理和更新
 *
 * @author luna
 * @date 2025/01/23
 */
@Slf4j
@Component
@Primary
public class TestDeviceSupplier implements DeviceSupplier {

    private static final String       LOOP_IP   = "127.0.0.1";

    /**
     * 用户ID到设备的映射
     * 统一存储Device对象，避免数据覆盖
     */
    private final Map<String, Device> userIdMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeDevices() {
        log.info("初始化测试设备配置...");

        // 客户端设备配置 - 统一存储为Device对象
        Device clientFrom = createClientFromDevice();
        addOrUpdateDevice(clientFrom);

        Device clientTo = createClientToDevice();
        addOrUpdateDevice(clientTo);

        // 服务端设备配置 - 统一存储为Device对象
        Device serverFrom = createServerFromDevice();
        addOrUpdateDevice(serverFrom);

        Device serverTo = createServerToDevice();
        addOrUpdateDevice(serverTo);

        log.info("测试设备初始化完成，共初始化 {} 个设备", getDeviceCount());
    }

    /**
     * 创建客户端From设备
     */
    private Device createClientFromDevice() {
        return FromDevice.getInstance("33010602011187000001", LOOP_IP, 8118);
    }

    /**
     * 创建客户端To设备
     */
    private Device createClientToDevice() {
        ToDevice device = ToDevice.getInstance("41010500002000000001", LOOP_IP, 8117);
        device.setPassword("bajiuwulian1006");
        device.setRealm("4101050000");
        return device;
    }

    /**
     * 创建服务端From设备
     */
    private Device createServerFromDevice() {
        FromDevice device = FromDevice.getInstance("41010500002000000001", LOOP_IP, 8117);
        device.setPassword("bajiuwulian1006");
        device.setRealm("4101050000");
        return device;
    }

    /**
     * 创建服务端To设备
     */
    private Device createServerToDevice() {
        ToDevice device = ToDevice.getInstance("33010602011187000001", LOOP_IP, 8118);
        return device;
    }

    @Override
    public List<Device> getDevices() {
        return Lists.newArrayList(userIdMap.values());
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

        // 更新用户ID映射 - 统一存储Device对象
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

    @Override
    public String getName() {
        return "TestDeviceSupplier";
    }

    // ==================== 转换方法 ====================

    /**
     * 获取客户端From设备
     * 用于客户端主动发送请求的场景
     */
    public FromDevice getClientFromDevice() {
        Device device = getDevice("33010602011187000001");
        if (device instanceof FromDevice) {
            return (FromDevice)device;
        }
        // 如果不是FromDevice类型，创建新的FromDevice
        return createFromDevice(device);
    }

    /**
     * 获取客户端To设备
     * 用于客户端接收响应的场景
     */
    public ToDevice getClientToDevice() {
        Device device = getDevice("41010500002000000001");
        if (device instanceof ToDevice) {
            return (ToDevice)device;
        }
        // 如果不是ToDevice类型，创建新的ToDevice
        return createToDevice(device);
    }

    /**
     * 获取服务端From设备
     * 用于服务端主动发送请求的场景
     */
    public FromDevice getServerFromDevice() {
        Device device = getDevice("41010500002000000001");
        if (device instanceof FromDevice) {
            return (FromDevice)device;
        }
        // 如果不是FromDevice类型，创建新的FromDevice
        return createFromDevice(device);
    }

    /**
     * 获取服务端To设备
     * 用于服务端接收响应的场景
     */
    public ToDevice getServerToDevice() {
        Device device = getDevice("33010602011187000001");
        if (device instanceof ToDevice) {
            return (ToDevice)device;
        }
        // 如果不是ToDevice类型，创建新的ToDevice
        return createToDevice(device);
    }

    /**
     * 根据Device创建FromDevice
     */
    public FromDevice createFromDevice(Device device) {
        if (device == null) {
            log.warn("设备为空，无法创建FromDevice");
            return null;
        }

        FromDevice fromDevice = FromDevice.getInstance(device.getUserId(), device.getIp(), device.getPort());
        fromDevice.setPassword(device.getPassword());
        fromDevice.setRealm(device.getRealm());
        fromDevice.setTransport(device.getTransport());
        fromDevice.setStreamMode(device.getStreamMode());
        fromDevice.setCharset(device.getCharset());
        return fromDevice;
    }

    /**
     * 根据Device创建ToDevice
     */
    public ToDevice createToDevice(Device device) {
        if (device == null) {
            log.warn("设备为空，无法创建ToDevice");
            return null;
        }

        ToDevice toDevice = ToDevice.getInstance(device.getUserId(), device.getIp(), device.getPort());
        toDevice.setPassword(device.getPassword());
        toDevice.setRealm(device.getRealm());
        toDevice.setTransport(device.getTransport());
        toDevice.setStreamMode(device.getStreamMode());
        toDevice.setCharset(device.getCharset());
        return toDevice;
    }

    /**
     * 获取所有FromDevice类型的设备
     */
    public List<FromDevice> getFromDevices() {
        return userIdMap.values().stream()
            .filter(device -> device instanceof FromDevice)
            .map(device -> (FromDevice)device)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有ToDevice类型的设备
     */
    public List<ToDevice> getToDevices() {
        return userIdMap.values().stream()
            .filter(device -> device instanceof ToDevice)
            .map(device -> (ToDevice)device)
            .collect(Collectors.toList());
    }
}