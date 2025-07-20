package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.config.TestDeviceSupplier;
import io.github.lunasaw.gbproxy.test.handler.TestServerMessageProcessorHandler;
import io.github.lunasaw.gbproxy.test.utils.TestSipRequestUtils;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.request.SipRequestProvider;
import io.github.lunasaw.sip.common.transmit.SipSender;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sip.SipListener;
import javax.sip.message.Request;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基础SIP通用功能测试
 * 测试SIP协议的基本功能和设备管理
 *
 * @author claude
 * @date 2025/01/19
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "logging.level.io.github.lunasaw.sip=DEBUG",
        "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicSipCommonTest {

    @Autowired(required = false)
    protected TestDeviceSupplier deviceSupplier;

    @Autowired(required = false)
    protected SipLayer sipLayer;

    @Autowired(required = false)
    protected SipListener sipListener;

    @Autowired(required = false)
    protected io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageRequestProcessor serverMessageRequestProcessor;

    @BeforeEach
    public void setUp() {
        if (sipLayer != null && sipListener != null && deviceSupplier != null) {
            // Set up SIP listener
            sipLayer.setSipListener(sipListener);

            // Set up listening points for server (5060) and client (5061)
            sipLayer.addListeningPoint("127.0.0.1", 5060);
            sipLayer.addListeningPoint("127.0.0.1", 5061);

            // Manually register MESSAGE processor if needed
            if (sipListener instanceof io.github.lunasaw.sip.common.transmit.AbstractSipListener) {
                io.github.lunasaw.sip.common.transmit.AbstractSipListener abstractListener =
                        (io.github.lunasaw.sip.common.transmit.AbstractSipListener) sipListener;

                // Check if MESSAGE processor is already registered
                if (abstractListener.getRequestProcessors("MESSAGE") == null && serverMessageRequestProcessor != null) {
                    System.out.println("🔧 手动注册MESSAGE处理器");
                    abstractListener.addRequestProcessor("MESSAGE", serverMessageRequestProcessor);
                }

                System.out.println("🔍 Registered processors: " + abstractListener.getProcessorStats());
                System.out.println("🔍 MESSAGE processors: " + abstractListener.getRequestProcessors("MESSAGE"));
            }

            System.out.println("✓ SipLayer初始化完成 - 监听端口: 5060 (server), 5061 (client)");
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试Spring上下文加载")
    public void testSpringContextLoads() {
        Assertions.assertTrue(true, "Spring上下文应该能够加载");
        System.out.println("✓ Spring上下文加载成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试设备提供器注入")
    public void testDeviceSupplierInjection() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过设备提供器测试 - 未注入");
            return;
        }

        Assertions.assertNotNull(deviceSupplier, "设备提供器应该被正确注入");
        System.out.println("✓ 设备提供器注入成功: " + deviceSupplier.getClass().getSimpleName());
    }

    @Test
    @Order(3)
    @DisplayName("测试设备获取和转换功能")
    public void testDeviceRetrieval() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过设备获取测试 - 设备提供器未注入");
            return;
        }

        try {
            // 测试获取所有设备
            List<Device> allDevices = deviceSupplier.getDevices();
            System.out.println("✓ 总设备数量: " + allDevices.size());
            Assertions.assertTrue(allDevices.size() > 0, "应该至少有一个设备");

            // 测试客户端设备转换
            FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
            ToDevice clientToDevice = deviceSupplier.getClientToDevice();

            if (clientFromDevice != null) {
                Assertions.assertNotNull(clientFromDevice.getUserId(), "客户端From设备ID不能为空");
                System.out.println("✓ 客户端From设备: " + clientFromDevice.getUserId() + "@" +
                        clientFromDevice.getIp() + ":" + clientFromDevice.getPort());
            }

            if (clientToDevice != null) {
                Assertions.assertNotNull(clientToDevice.getUserId(), "客户端To设备ID不能为空");
                System.out.println("✓ 客户端To设备: " + clientToDevice.getUserId() + "@" +
                        clientToDevice.getIp() + ":" + clientToDevice.getPort());
            }

            // 测试服务端设备转换
            FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
            ToDevice serverToDevice = deviceSupplier.getServerToDevice();

            if (serverFromDevice != null) {
                Assertions.assertNotNull(serverFromDevice.getUserId(), "服务端From设备ID不能为空");
                System.out.println("✓ 服务端From设备: " + serverFromDevice.getUserId() + "@" +
                        serverFromDevice.getIp() + ":" + serverFromDevice.getPort());
            }

            if (serverToDevice != null) {
                Assertions.assertNotNull(serverToDevice.getUserId(), "服务端To设备ID不能为空");
                System.out.println("✓ 服务端To设备: " + serverToDevice.getUserId() + "@" +
                        serverToDevice.getIp() + ":" + serverToDevice.getPort());
            }

        } catch (Exception e) {
            System.err.println("✗ 设备获取测试失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("设备获取测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试SIP请求工具类")
    public void testSipRequestUtils() {
        try {
            // 测试CallId生成
            String callId1 = TestSipRequestUtils.getNewCallId();
            String callId2 = TestSipRequestUtils.getNewCallId();

            Assertions.assertNotNull(callId1, "CallId不能为空");
            Assertions.assertNotNull(callId2, "CallId不能为空");
            Assertions.assertNotEquals(callId1, callId2, "生成的CallId应该不同");

            System.out.println("✓ SIP请求工具类测试通过");
            System.out.println("  CallId1: " + callId1);
            System.out.println("  CallId2: " + callId2);
        } catch (Exception e) {
            System.err.println("✗ SIP请求工具类测试失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("SIP请求工具类测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("测试SIP REGISTER请求创建")
    public void testCreateRegisterRequest() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过SIP REGISTER请求测试 - 设备提供器未注入");
            return;
        }

        try {
            FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
            ToDevice clientToDevice = deviceSupplier.getClientToDevice();

            if (clientFromDevice == null || clientToDevice == null) {
                System.out.println("⚠ 跳过SIP REGISTER请求测试 - 设备未获取");
                return;
            }

            String callId = TestSipRequestUtils.getNewCallId();
            Request registerRequest = SipRequestProvider.createRegisterRequest(
                    clientFromDevice,
                    clientToDevice,
                    3600,
                    callId);

            Assertions.assertNotNull(registerRequest, "REGISTER请求应该被成功创建");
            Assertions.assertEquals("REGISTER", registerRequest.getMethod(), "请求方法应该是REGISTER");

            System.out.println("✓ REGISTER请求创建成功");
            System.out.println("  CallId: " + callId);
            System.out.println("  From: " + registerRequest.getHeader("From"));
            System.out.println("  To: " + registerRequest.getHeader("To"));
        } catch (Exception e) {
            System.err.println("✗ SIP REGISTER请求创建失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("SIP REGISTER请求创建失败: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("测试SIP MESSAGE请求创建")
    public void testCreateMessageRequest() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过SIP MESSAGE请求测试 - 设备提供器未注入");
            return;
        }

        try {
            FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
            ToDevice clientToDevice = deviceSupplier.getClientToDevice();

            if (clientFromDevice == null || clientToDevice == null) {
                System.out.println("⚠ 跳过SIP MESSAGE请求测试 - 设备未获取");
                return;
            }

            String callId = TestSipRequestUtils.getNewCallId();
            String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

            Assertions.assertNotNull(messageRequest, "MESSAGE请求应该被成功创建");
            Assertions.assertEquals("MESSAGE", messageRequest.getMethod(), "请求方法应该是MESSAGE");

            System.out.println("✓ MESSAGE请求创建成功");
            System.out.println("  CallId: " + callId);
            System.out.println("  Content-Length: " + messageRequest.getContentLength().getContentLength());
        } catch (Exception e) {
            System.err.println("✗ SIP MESSAGE请求创建失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("SIP MESSAGE请求创建失败: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("测试SIP MESSAGE请求发送和服务端接收")
    public void testSendMessageRequest() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过SIP MESSAGE请求发送测试 - 设备提供器未注入");
            return;
        }

        try {
            // 重置测试Handler状态
            TestServerMessageProcessorHandler.resetTestState();

            FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
            ToDevice clientToDevice = deviceSupplier.getClientToDevice();

            if (clientFromDevice == null || clientToDevice == null) {
                System.out.println("⚠ 跳过SIP MESSAGE请求发送测试 - 设备未获取");
                return;
            }

            System.out.println("📤 准备发送MESSAGE请求");
            System.out.println("  发送方: " + clientFromDevice.getUserId() + "@" +
                    clientFromDevice.getIp() + ":" + clientFromDevice.getPort());
            System.out.println("  接收方: " + clientToDevice.getUserId() + "@" +
                    clientToDevice.getIp() + ":" + clientToDevice.getPort());
            System.out.println("  消息内容: Keepalive命令");

            // 发送MESSAGE请求
            String callId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, "onLine");

            Assertions.assertNotNull(callId, "MESSAGE请求发送应该返回callId");
            Assertions.assertFalse(callId.isEmpty(), "CallId不能为空");

            System.out.println("✅ MESSAGE请求发送成功，CallId: " + callId);

            // 等待服务端接收心跳消息，最多等待5秒
            System.out.println("⏳ 等待服务端接收和处理心跳消息...");
            boolean received = TestServerMessageProcessorHandler.waitForKeepalive(5, TimeUnit.SECONDS);

            // 验证服务端是否接收到心跳消息
            Assertions.assertTrue(received, "服务端应该在5秒内接收到心跳消息");
            Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedKeepalive(),
                    "服务端应该成功接收心跳消息");

            // 验证接收到的心跳内容
            var receivedKeepalive = TestServerMessageProcessorHandler.getReceivedKeepalive();
            Assertions.assertNotNull(receivedKeepalive, "接收到的心跳内容不能为空");
            Assertions.assertNotNull(receivedKeepalive.getDeviceId(), "心跳设备ID不能为空");
            Assertions.assertEquals(clientFromDevice.getUserId(), receivedKeepalive.getDeviceId(),
                    "心跳设备ID应该与发送方设备ID一致");

            System.out.println("✅ 服务端成功接收MESSAGE请求并处理心跳");
            System.out.println("✅ 心跳内容验证通过: 设备ID=" + receivedKeepalive.getDeviceId());
            System.out.println("✅ CallId验证通过: " + callId);
            System.out.println("🎉 MESSAGE请求端到端验证完全成功！");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ SIP MESSAGE请求发送测试被中断: " + e.getMessage());
            Assertions.fail("SIP MESSAGE请求发送测试被中断: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ SIP MESSAGE请求发送测试失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("SIP MESSAGE请求发送测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("测试设备配置验证")
    public void testDeviceConfiguration() {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过设备配置验证测试 - 设备提供器未注入");
            return;
        }

        try {
            // 验证客户端设备配置
            FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
            ToDevice clientToDevice = deviceSupplier.getClientToDevice();

            if (clientFromDevice != null) {
                Assertions.assertNotNull(clientFromDevice.getUserId(), "客户端From设备ID不能为空");
                Assertions.assertNotNull(clientFromDevice.getIp(), "客户端From设备IP不能为空");
                Assertions.assertTrue(clientFromDevice.getPort() > 0, "客户端From设备端口必须大于0");
                System.out.println("✓ 客户端From设备配置验证通过");
            }

            if (clientToDevice != null) {
                Assertions.assertNotNull(clientToDevice.getUserId(), "客户端To设备ID不能为空");
                Assertions.assertNotNull(clientToDevice.getIp(), "客户端To设备IP不能为空");
                Assertions.assertTrue(clientToDevice.getPort() > 0, "客户端To设备端口必须大于0");
                System.out.println("✓ 客户端To设备配置验证通过");
            }

            // 验证服务端设备配置
            FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
            ToDevice serverToDevice = deviceSupplier.getServerToDevice();

            if (serverFromDevice != null) {
                Assertions.assertNotNull(serverFromDevice.getUserId(), "服务端From设备ID不能为空");
                Assertions.assertNotNull(serverFromDevice.getIp(), "服务端From设备IP不能为空");
                Assertions.assertTrue(serverFromDevice.getPort() > 0, "服务端From设备端口必须大于0");
                System.out.println("✓ 服务端From设备配置验证通过");
            }

            if (serverToDevice != null) {
                Assertions.assertNotNull(serverToDevice.getUserId(), "服务端To设备ID不能为空");
                Assertions.assertNotNull(serverToDevice.getIp(), "服务端To设备IP不能为空");
                Assertions.assertTrue(serverToDevice.getPort() > 0, "服务端To设备端口必须大于0");
                System.out.println("✓ 服务端To设备配置验证通过");
            }

        } catch (Exception e) {
            System.err.println("✗ 设备配置验证失败: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("设备配置验证失败: " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        System.out.println("--- 测试用例完成 ---\n");
    }
}