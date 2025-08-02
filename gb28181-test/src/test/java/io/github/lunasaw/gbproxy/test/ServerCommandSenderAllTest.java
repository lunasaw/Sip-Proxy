package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.handler.TestClientMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * MESSAGE 类型 的消息
 * 服务端主动命令全量测试（只测ServerCommandSender定义的服务端主动命令）
 *
 * @author AI
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "logging.level.io.github.lunasaw.sip=DEBUG",
        "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerCommandSenderAllTest extends BasicSipCommonTest {

    @Test
    @Order(1)
    @DisplayName("测试 deviceInfoQuery（设备信息查询）")
    public void testDeviceInfoQuery() throws Exception {
        TestClientMessageProcessorHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        String callId = ServerCommandSender.deviceInfoQuery(serverFromDevice, serverToDevice);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestClientMessageProcessorHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "客户端应收到设备信息查询请求");
        // 可断言客户端响应内容
        DeviceInfo deviceInfo = TestClientMessageProcessorHandler.getReceivedDeviceInfo();
        Assertions.assertNotNull(deviceInfo, "设备信息响应不能为空");
    }

    @Test
    @Order(2)
    @DisplayName("测试 deviceStatusQuery（设备状态查询）")
    public void testDeviceStatusQuery() throws Exception {
        TestClientMessageProcessorHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        String callId = ServerCommandSender.deviceStatusQuery(serverFromDevice, serverToDevice);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestClientMessageProcessorHandler.waitForDeviceStatus(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "客户端应收到设备状态查询请求");
        Assertions.assertNotNull(TestClientMessageProcessorHandler.getReceivedDeviceStatus(), "设备状态响应不能为空");
    }

    @Test
    @Order(3)
    @DisplayName("测试 deviceCatalogQuery（设备目录查询）")
    public void testDeviceCatalogQuery() throws Exception {
        TestClientMessageProcessorHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        String callId = ServerCommandSender.deviceCatalogQuery(serverFromDevice, serverToDevice);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestClientMessageProcessorHandler.waitForCatalog(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "客户端应收到设备目录查询请求");
        Assertions.assertNotNull(TestClientMessageProcessorHandler.getReceivedCatalog(), "设备目录响应不能为空");
    }

    @Test
    @Order(4)
    @DisplayName("测试 deviceRecordInfoQuery（录像信息查询）")
    public void testDeviceRecordInfoQuery() throws Exception {
        TestClientMessageProcessorHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        String callId = ServerCommandSender.deviceRecordInfoQuery(serverFromDevice, serverToDevice, new Date(), new Date());
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestClientMessageProcessorHandler.waitForDeviceRecord(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "客户端应收到录像信息查询请求");
        Assertions.assertNotNull(TestClientMessageProcessorHandler.getReceivedDeviceRecord(), "录像响应不能为空");
    }

    @Test
    @Order(5)
    @DisplayName("测试 deviceMobilePositionQuery（移动位置查询）")
    public void testDeviceMobilePositionQuery() throws Exception {
        TestClientMessageProcessorHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        String callId = ServerCommandSender.deviceMobilePositionQuery(serverFromDevice, serverToDevice, "60");
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestClientMessageProcessorHandler.waitForMobilePosition(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "客户端应收到移动位置查询请求");
        Assertions.assertNotNull(TestClientMessageProcessorHandler.getReceivedMobilePosition(), "移动位置响应不能为空");
    }


}