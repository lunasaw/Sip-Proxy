package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.sip.RequestEvent;
import java.util.List;

/**
 * 测试专用的服务端设备提供器
 * 覆盖默认实现，提供测试环境的设备配置
 */
@Service
@Primary
@Slf4j
public class TestServerDeviceSupplier implements ServerDeviceSupplier {

    @Autowired
    private TestDeviceSupplier testDeviceSupplier;

    @Override
    public List<Device> getDevices() {
        return testDeviceSupplier.getDevices();
    }

    @Override
    public Device getDevice(String userId) {
        log.info("🔍 TestServerDeviceSupplier 查找设备: {}", userId);
        Device device = testDeviceSupplier.getDevice(userId);
        if (device != null) {
            log.info("✅ TestServerDeviceSupplier 找到设备: {}", device.getUserId());
        } else {
            log.warn("❌ TestServerDeviceSupplier 未找到设备: {}", userId);
        }
        return device;
    }

    @Override
    public void addOrUpdateDevice(Device device) {
        testDeviceSupplier.addOrUpdateDevice(device);
    }

    @Override
    public void removeDevice(String userId) {
        testDeviceSupplier.removeDevice(userId);
    }

    @Override
    public int getDeviceCount() {
        return testDeviceSupplier.getDeviceCount();
    }

    @Override
    public FromDevice getServerFromDevice() {
        return testDeviceSupplier.getServerFromDevice();
    }

    @Override
    public void setServerFromDevice(FromDevice fromDevice) {
        // 不实现，使用TestDeviceSupplier的逻辑
    }

    @Override
    public String getName() {
        return "TestServerDeviceSupplier";
    }

    @Override
    public boolean checkDevice(RequestEvent event) {
        // 对于测试环境，总是返回true，允许所有设备通过
        log.info("✅ TestServerDeviceSupplier 设备检查通过（测试模式）");
        return true;
    }
}