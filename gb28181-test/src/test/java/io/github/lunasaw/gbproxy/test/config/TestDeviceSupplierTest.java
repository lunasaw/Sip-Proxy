package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestDeviceSupplier测试类
 * 验证重构后的设备提供器功能
 *
 * @author luna
 * @date 2025/01/23
 */
@SpringBootTest(classes = TestDeviceSupplier.class)
@DisplayName("TestDeviceSupplier测试")
public class TestDeviceSupplierTest {

    @Autowired
    private TestDeviceSupplier testDeviceSupplier;

    @BeforeEach
    public void setUp() {
        // 确保设备已初始化
        assertNotNull(testDeviceSupplier, "TestDeviceSupplier应该被正确注入");
    }

    @Test
    @DisplayName("测试设备初始化")
    public void testDeviceInitialization() {
        // 验证设备数量
        int deviceCount = testDeviceSupplier.getDeviceCount();
        assertEquals(2, deviceCount, "应该初始化2个设备");

        // 验证所有设备都能获取到
        Device device1 = testDeviceSupplier.getDevice("33010602011187000001");
        Device device2 = testDeviceSupplier.getDevice("41010500002000000001");

        assertNotNull(device1, "设备1应该存在");
        assertNotNull(device2, "设备2应该存在");

        System.out.println("设备1: " + device1.getUserId() + "@" + device1.getIp() + ":" + device1.getPort());
        System.out.println("设备2: " + device2.getUserId() + "@" + device2.getIp() + ":" + device2.getPort());
    }

    @Test
    @DisplayName("测试客户端From设备转换")
    public void testClientFromDeviceConversion() {
        FromDevice clientFrom = testDeviceSupplier.getClientFromDevice();

        assertNotNull(clientFrom, "客户端From设备不能为空");
        assertEquals("33010602011187000001", clientFrom.getUserId(), "用户ID应该正确");
        assertEquals("127.0.0.1", clientFrom.getIp(), "IP地址应该正确");
        assertEquals(8118, clientFrom.getPort(), "端口应该正确");
        assertNotNull(clientFrom.getFromTag(), "FromTag应该存在");
        assertNotNull(clientFrom.getAgent(), "Agent应该存在");

        System.out.println("客户端From设备: " + clientFrom.getUserId() + "@" + clientFrom.getIp() + ":" + clientFrom.getPort());
        System.out.println("FromTag: " + clientFrom.getFromTag());
        System.out.println("Agent: " + clientFrom.getAgent());
    }

    @Test
    @DisplayName("测试客户端To设备转换")
    public void testClientToDeviceConversion() {
        ToDevice clientTo = testDeviceSupplier.getClientToDevice();

        assertNotNull(clientTo, "客户端To设备不能为空");
        assertEquals("41010500002000000001", clientTo.getUserId(), "用户ID应该正确");
        assertEquals("127.0.0.1", clientTo.getIp(), "IP地址应该正确");
        assertEquals(8117, clientTo.getPort(), "端口应该正确");
        assertEquals("bajiuwulian1006", clientTo.getPassword(), "密码应该正确");
        assertEquals("4101050000", clientTo.getRealm(), "Realm应该正确");

        System.out.println("客户端To设备: " + clientTo.getUserId() + "@" + clientTo.getIp() + ":" + clientTo.getPort());
        System.out.println("Password: " + clientTo.getPassword());
        System.out.println("Realm: " + clientTo.getRealm());
    }

    @Test
    @DisplayName("测试服务端From设备转换")
    public void testServerFromDeviceConversion() {
        FromDevice serverFrom = testDeviceSupplier.getServerFromDevice();

        assertNotNull(serverFrom, "服务端From设备不能为空");
        assertEquals("41010500002000000001", serverFrom.getUserId(), "用户ID应该正确");
        assertEquals("127.0.0.1", serverFrom.getIp(), "IP地址应该正确");
        assertEquals(8117, serverFrom.getPort(), "端口应该正确");
        assertEquals("bajiuwulian1006", serverFrom.getPassword(), "密码应该正确");
        assertEquals("4101050000", serverFrom.getRealm(), "Realm应该正确");
        assertNotNull(serverFrom.getFromTag(), "FromTag应该存在");

        System.out.println("服务端From设备: " + serverFrom.getUserId() + "@" + serverFrom.getIp() + ":" + serverFrom.getPort());
        System.out.println("Password: " + serverFrom.getPassword());
        System.out.println("Realm: " + serverFrom.getRealm());
        System.out.println("FromTag: " + serverFrom.getFromTag());
    }

    @Test
    @DisplayName("测试服务端To设备转换")
    public void testServerToDeviceConversion() {
        ToDevice serverTo = testDeviceSupplier.getServerToDevice();

        assertNotNull(serverTo, "服务端To设备不能为空");
        assertEquals("33010602011187000001", serverTo.getUserId(), "用户ID应该正确");
        assertEquals("127.0.0.1", serverTo.getIp(), "IP地址应该正确");
        assertEquals(8118, serverTo.getPort(), "端口应该正确");

        System.out.println("服务端To设备: " + serverTo.getUserId() + "@" + serverTo.getIp() + ":" + serverTo.getPort());
    }

    @Test
    @DisplayName("测试设备类型转换")
    public void testDeviceTypeConversion() {
        // 测试从Device转换为FromDevice
        Device device = testDeviceSupplier.getDevice("33010602011187000001");
        assertNotNull(device, "设备应该存在");

        FromDevice fromDevice = testDeviceSupplier.getClientFromDevice();
        assertNotNull(fromDevice, "FromDevice转换应该成功");
        assertEquals(device.getUserId(), fromDevice.getUserId(), "用户ID应该一致");
        assertEquals(device.getIp(), fromDevice.getIp(), "IP地址应该一致");
        assertEquals(device.getPort(), fromDevice.getPort(), "端口应该一致");

        // 测试从Device转换为ToDevice
        Device device2 = testDeviceSupplier.getDevice("41010500002000000001");
        assertNotNull(device2, "设备2应该存在");

        ToDevice toDevice = testDeviceSupplier.getClientToDevice();
        assertNotNull(toDevice, "ToDevice转换应该成功");
        assertEquals(device2.getUserId(), toDevice.getUserId(), "用户ID应该一致");
        assertEquals(device2.getIp(), toDevice.getIp(), "IP地址应该一致");
        assertEquals(device2.getPort(), toDevice.getPort(), "端口应该一致");
    }

    @Test
    @DisplayName("测试设备添加和更新")
    public void testDeviceAddAndUpdate() {
        // 添加新设备
        FromDevice newDevice = FromDevice.getInstance("test-device-001", "192.168.1.100", 8080);
        testDeviceSupplier.addOrUpdateDevice(newDevice);

        // 验证设备已添加
        Device retrievedDevice = testDeviceSupplier.getDevice("test-device-001");
        assertNotNull(retrievedDevice, "新添加的设备应该存在");
        assertEquals("test-device-001", retrievedDevice.getUserId(), "用户ID应该正确");

        // 更新设备
        ToDevice updatedDevice = ToDevice.getInstance("test-device-001", "192.168.1.200", 8081);
        updatedDevice.setPassword("new-password");
        testDeviceSupplier.addOrUpdateDevice(updatedDevice);

        // 验证设备已更新
        Device retrievedUpdatedDevice = testDeviceSupplier.getDevice("test-device-001");
        assertNotNull(retrievedUpdatedDevice, "更新后的设备应该存在");
        assertEquals("192.168.1.200", retrievedUpdatedDevice.getIp(), "IP地址应该已更新");
        assertEquals(8081, retrievedUpdatedDevice.getPort(), "端口应该已更新");

        // 清理测试设备
        testDeviceSupplier.removeDevice("test-device-001");
        Device removedDevice = testDeviceSupplier.getDevice("test-device-001");
        assertNull(removedDevice, "移除后的设备应该不存在");

        System.out.println("设备添加、更新、移除测试通过");
    }

    @Test
    @DisplayName("测试设备类型统计")
    public void testDeviceTypeStatistics() {
        // 获取所有FromDevice
        List<FromDevice> fromDevices = testDeviceSupplier.getFromDevices();
        assertNotNull(fromDevices, "FromDevice列表不能为空");
        assertTrue(fromDevices.size() >= 1, "应该至少有1个FromDevice");

        // 获取所有ToDevice
        List<ToDevice> toDevices = testDeviceSupplier.getToDevices();
        assertNotNull(toDevices, "ToDevice列表不能为空");
        assertTrue(toDevices.size() >= 1, "应该至少有1个ToDevice");

        System.out.println("FromDevice数量: " + fromDevices.size());
        System.out.println("ToDevice数量: " + toDevices.size());

        // 验证设备类型
        fromDevices.forEach(device -> {
            assertTrue(device instanceof FromDevice, "应该是FromDevice类型");
            System.out.println("FromDevice: " + device.getUserId());
        });

        toDevices.forEach(device -> {
            assertTrue(device instanceof ToDevice, "应该是ToDevice类型");
            System.out.println("ToDevice: " + device.getUserId());
        });
    }

    @Test
    @DisplayName("测试设备提供器名称")
    public void testDeviceSupplierName() {
        String name = testDeviceSupplier.getName();
        assertEquals("TestDeviceSupplier", name, "设备提供器名称应该正确");
        System.out.println("设备提供器名称: " + name);
    }
}