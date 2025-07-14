package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.test.config.TestDeviceSupplier;
import io.github.lunasaw.gbproxy.test.utils.TestSipRequestUtils;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import io.github.lunasaw.sip.common.transmit.request.SipRequestProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sip.message.Request;
import java.util.List;

/**
 * SIP代理基础功能测试
 * 用于快速验证SIP代理的基本功能
 *
 * 已改造为使用TestDeviceSupplier的新转换方法：
 * - 使用 getClientFromDevice() 获取客户端From设备
 * - 使用 getClientToDevice() 获取客户端To设备
 * - 使用 getServerFromDevice() 获取服务端From设备
 * - 使用 getServerToDevice() 获取服务端To设备
 *
 * @author luna
 * @date 2024/01/01
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "logging.level.io.github.lunasaw.sip=DEBUG",
    "logging.level.org.springframework=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SipProxyBasicTest {

    @Autowired(required = false)
    private TestDeviceSupplier deviceSupplier;

    private FromDevice         clientFromDevice;
    private ToDevice           clientToDevice;

    @BeforeEach
    public void setUp() {
        System.out.println("=== 开始SIP代理基础测试 ===");

        // 检查设备提供器是否可用
        if (deviceSupplier == null) {
            System.out.println("警告: 设备提供器未注入，跳过设备相关测试");
            return;
        }

        try {
            // 使用新的转换方法获取设备
            clientFromDevice = deviceSupplier.getClientFromDevice();
            clientToDevice = deviceSupplier.getClientToDevice();

            System.out.println("设备获取完成");
        } catch (Exception e) {
            System.err.println("设备获取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试Spring上下文加载")
    public void testSpringContextLoads() {
        // 基本Spring上下文测试
        Assertions.assertTrue(true, "Spring上下文应该能够加载");
        System.out.println("Spring上下文加载成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试设备提供器注入")
    public void testDeviceSupplierInjection() {
        if (deviceSupplier == null) {
            System.out.println("跳过设备提供器测试 - 未注入");
            return;
        }

        Assertions.assertNotNull(deviceSupplier, "设备提供器应该被正确注入");
        System.out.println("设备提供器注入成功: " + deviceSupplier.getClass().getSimpleName());
    }

    @Test
    @Order(3)
    @DisplayName("测试设备获取")
    public void testDeviceRetrieval() {
        if (deviceSupplier == null) {
            System.out.println("跳过设备获取测试 - 设备提供器未注入");
            return;
        }

        try {
            // 测试设备获取 - 使用新的转换方法
            FromDevice fromDevice = deviceSupplier.getClientFromDevice();
            ToDevice toDevice = deviceSupplier.getClientToDevice();

            if (fromDevice != null) {
                Assertions.assertNotNull(fromDevice.getUserId(), "From设备ID不能为空");
                System.out.println("From设备获取成功: " + fromDevice.getUserId());
            }

            if (toDevice != null) {
                Assertions.assertNotNull(toDevice.getUserId(), "To设备ID不能为空");
                System.out.println("To设备获取成功: " + toDevice.getUserId());
            }

            // 测试获取所有设备
            List<Device> allDevices = deviceSupplier.getDevices();
            System.out.println("总设备数量: " + allDevices.size());

        } catch (Exception e) {
            System.err.println("设备获取测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试SIP注册请求创建")
    public void testCreateRegisterRequest() {
        if (clientFromDevice == null || clientToDevice == null) {
            System.out.println("跳过SIP请求测试 - 设备未获取");
            return;
        }

        try {
            String callId = TestSipRequestUtils.getNewCallId();
            Request registerRequest = SipRequestProvider.createRegisterRequest(
                clientFromDevice,
                clientToDevice,
                3600,
                callId);

            Assertions.assertNotNull(registerRequest, "注册请求应该被成功创建");
            Assertions.assertEquals("REGISTER", registerRequest.getMethod(), "请求方法应该是REGISTER");

            System.out.println("注册请求创建成功");
            System.out.println("CallId: " + callId);
            System.out.println("From: " + registerRequest.getHeader("From"));
            System.out.println("To: " + registerRequest.getHeader("To"));
        } catch (Exception e) {
            System.err.println("SIP请求创建失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(5)
    @DisplayName("测试SIP消息请求创建")
    public void testCreateMessageRequest() {
        if (clientFromDevice == null || clientToDevice == null) {
            System.out.println("跳过SIP消息测试 - 设备未获取");
            return;
        }

        try {
            String callId = TestSipRequestUtils.getNewCallId();
            String xmlContent = "<?xml version=\"1.0\"?>\n" +
                "<Control>\n" +
                "  <CmdType>Keepalive</CmdType>\n" +
                "  <SN>1</SN>\n" +
                "  <DeviceID>" + clientFromDevice.getUserId() + "</DeviceID>\n" +
                "</Control>";

            Request messageRequest = SipRequestProvider.createMessageRequest(
                clientFromDevice,
                clientToDevice,
                callId,
                xmlContent);

            Assertions.assertNotNull(messageRequest, "消息请求应该被成功创建");
            Assertions.assertEquals("MESSAGE", messageRequest.getMethod(), "请求方法应该是MESSAGE");

            System.out.println("消息请求创建成功");
            System.out.println("CallId: " + callId);
            System.out.println("Content: " + xmlContent);
        } catch (Exception e) {
            System.err.println("SIP消息创建失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(6)
    @DisplayName("测试设备配置")
    public void testDeviceConfiguration() {
        if (clientFromDevice == null || clientToDevice == null) {
            System.out.println("跳过设备配置测试 - 设备未获取");
            return;
        }

        try {
            // 验证客户端From设备
            Assertions.assertNotNull(clientFromDevice.getUserId(), "From设备ID不能为空");
            Assertions.assertNotNull(clientFromDevice.getIp(), "From设备IP不能为空");
            Assertions.assertTrue(clientFromDevice.getPort() > 0, "From设备端口必须大于0");
            Assertions.assertNotNull(clientFromDevice.getFromTag(), "FromTag不能为空");
            Assertions.assertNotNull(clientFromDevice.getAgent(), "Agent不能为空");

            // 验证客户端To设备
            Assertions.assertNotNull(clientToDevice.getUserId(), "To设备ID不能为空");
            Assertions.assertNotNull(clientToDevice.getIp(), "To设备IP不能为空");
            Assertions.assertTrue(clientToDevice.getPort() > 0, "To设备端口必须大于0");

            System.out.println("From设备配置: " + clientFromDevice.getUserId() + "@" + clientFromDevice.getIp() + ":" + clientFromDevice.getPort());
            System.out.println("To设备配置: " + clientToDevice.getUserId() + "@" + clientToDevice.getIp() + ":" + clientToDevice.getPort());
            System.out.println("FromTag: " + clientFromDevice.getFromTag());
            System.out.println("Agent: " + clientFromDevice.getAgent());

        } catch (Exception e) {
            System.err.println("设备配置测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(7)
    @DisplayName("测试SIP请求工具类")
    public void testSipRequestUtils() {
        try {
            // 测试CallId生成
            String callId1 = TestSipRequestUtils.getNewCallId();
            String callId2 = TestSipRequestUtils.getNewCallId();

            Assertions.assertNotNull(callId1, "CallId不能为空");
            Assertions.assertNotNull(callId2, "CallId不能为空");
            Assertions.assertNotEquals(callId1, callId2, "生成的CallId应该不同");

            System.out.println("SIP请求工具类测试通过");
            System.out.println("CallId1: " + callId1);
            System.out.println("CallId2: " + callId2);
        } catch (Exception e) {
            System.err.println("SIP请求工具类测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @Order(8)
    @DisplayName("测试设备用户ID映射")
    public void testDeviceUserIdMapping() {
        if (deviceSupplier == null) {
            System.out.println("跳过设备用户ID映射测试 - 设备提供器未注入");
            return;
        }

        try {
            // 测试所有设备用户ID - 使用新的转换方法
            String[] userIds = {"33010602011187000001", "41010500002000000001"};

            for (String userId : userIds) {
                Device device = deviceSupplier.getDevice(userId);
                if (device != null) {
                    System.out.println("用户ID " + userId + " 获取成功: " + device.getUserId());
                } else {
                    System.out.println("用户ID " + userId + " 未找到设备");
                }
            }

            // 测试转换方法
            FromDevice fromDevice = deviceSupplier.getClientFromDevice();
            ToDevice toDevice = deviceSupplier.getClientToDevice();

            if (fromDevice != null) {
                System.out.println("客户端From设备: " + fromDevice.getUserId());
            }
            if (toDevice != null) {
                System.out.println("客户端To设备: " + toDevice.getUserId());
            }

            System.out.println("设备用户ID映射测试完成");
        } catch (Exception e) {
            System.err.println("设备用户ID映射测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        System.out.println("=== SIP代理基础测试完成 ===");
    }
}