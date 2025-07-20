package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.handler.TestServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

/**
 * 系统性测试ClientCommandSender所有指令
 * 参考testSendMessageRequest风格，断言完整
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
public class ClientCommandSenderAllTest extends BasicSipCommonTest {

    @Test
    @Order(1)
    @DisplayName("测试sendKeepaliveCommand（字符串）")
    public void testSendKeepaliveCommandString() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        String callId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, "onLine");
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForKeepalive(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到心跳");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedKeepalive(), "服务端应成功接收心跳");
        var receivedKeepalive = TestServerMessageProcessorHandler.getReceivedKeepalive();
        Assertions.assertNotNull(receivedKeepalive, "心跳内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedKeepalive.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(2)
    @DisplayName("测试sendKeepaliveCommand（DeviceKeepLiveNotify）")
    public void testSendKeepaliveCommandNotify() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        DeviceKeepLiveNotify notify = new DeviceKeepLiveNotify();
        notify.setDeviceId(clientFromDevice.getUserId());
        notify.setStatus("ON");
        String callId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, notify);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForKeepalive(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到心跳");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedKeepalive(), "服务端应成功接收心跳");
        var receivedKeepalive = TestServerMessageProcessorHandler.getReceivedKeepalive();
        Assertions.assertNotNull(receivedKeepalive, "心跳内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedKeepalive.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(3)
    @DisplayName("测试sendAlarmCommand（DeviceAlarmNotify）")
    public void testSendAlarmCommandNotify() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        // 构造DeviceAlarm对象，设置丰富字段
        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setDeviceId(clientFromDevice.getUserId());
        deviceAlarm.setAlarmPriority("1"); // 一级警情
        deviceAlarm.setAlarmMethod("2"); // 设备报警
        deviceAlarm.setAlarmTime(new java.util.Date());
        deviceAlarm.setAlarmDescription("测试报警信息");
        deviceAlarm.setLongitude(120.123456);
        deviceAlarm.setLatitude(30.123456);
        deviceAlarm.setAlarmType("1"); // 1-视频丢失报警
        // 构造DeviceAlarmNotify并用setAlarm填充
        DeviceAlarmNotify alarmNotify = new DeviceAlarmNotify();
        alarmNotify.setCmdType("Alarm");
        alarmNotify.setSn("123456");
        alarmNotify.setDeviceId(clientFromDevice.getUserId());
        alarmNotify.setAlarm(deviceAlarm);
        String callId = ClientCommandSender.sendAlarmCommand(clientFromDevice, clientToDevice, alarmNotify);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForAlarm(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到报警");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedAlarm(), "服务端应成功接收报警");
        var receivedAlarm = TestServerMessageProcessorHandler.getReceivedAlarm();
        Assertions.assertNotNull(receivedAlarm, "报警内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedAlarm.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(4)
    @DisplayName("测试sendCatalogCommand（DeviceResponse）")
    public void testSendCatalogCommandResponse() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        // 构造DeviceResponse，设置完整字段
        DeviceResponse response = new DeviceResponse();
        response.setCmdType("Catalog");
        response.setSn("654321");
        response.setDeviceId(clientFromDevice.getUserId());
        response.setSumNum(2);
        // 构造DeviceItem列表
        DeviceItem item1 = new DeviceItem();
        item1.setDeviceId(clientFromDevice.getUserId());
        item1.setName("测试通道1");
        item1.setManufacturer("测试厂商A");
        item1.setModel("Model-A");
        item1.setOwner("AdminA");
        item1.setCivilCode("440501");
        item1.setAddress("测试地址A");
        item1.setParental(0);
        item1.setParentId("0");
        item1.setSafetyWay(0);
        item1.setRegisterWay(1);
        item1.setSecrecy(0);
        item1.setStatus("ON");
        item1.setBlock("BlockA");
        item1.setCertNum("CertNumA");
        item1.setCertifiable(1);
        item1.setErrCode(0);
        item1.setEndTime("2099-01-01T01:01:01");
        item1.setIpAddress("192.168.1.101");
        item1.setPort(5060);
        item1.setPassword("passA");
        item1.setPtzType(1);
        item1.setLongitude(121.472644);
        item1.setLatitude(31.231706);
        DeviceItem item2 = new DeviceItem();
        item2.setDeviceId(clientFromDevice.getUserId().substring(0, 18) + "02");
        item2.setName("测试通道2");
        item2.setManufacturer("测试厂商B");
        item2.setModel("Model-B");
        item2.setOwner("AdminB");
        item2.setCivilCode("440502");
        item2.setAddress("测试地址B");
        item2.setParental(0);
        item2.setParentId("0");
        item2.setSafetyWay(0);
        item2.setRegisterWay(1);
        item2.setSecrecy(0);
        item2.setStatus("OFF");
        item2.setBlock("BlockB");
        item2.setCertNum("CertNumB");
        item2.setCertifiable(0);
        item2.setErrCode(1);
        item2.setEndTime("2099-12-31T23:59:59");
        item2.setIpAddress("192.168.1.102");
        item2.setPort(5061);
        item2.setPassword("passB");
        item2.setPtzType(2);
        item2.setLongitude(120.123456);
        item2.setLatitude(30.123456);
        response.setDeviceItemList(java.util.Arrays.asList(item1, item2));
        String callId = ClientCommandSender.sendCatalogCommand(clientFromDevice, clientToDevice, response);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForCatalog(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到目录");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedCatalog(), "服务端应成功接收目录");
        var receivedCatalog = TestServerMessageProcessorHandler.getReceivedCatalog();
        Assertions.assertNotNull(receivedCatalog, "目录内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedCatalog.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(6)
    @DisplayName("测试sendDeviceInfoCommand")
    public void testSendDeviceInfoCommand() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        DeviceInfo info = new DeviceInfo();
        info.setDeviceId(clientFromDevice.getUserId());
        String callId = ClientCommandSender.sendDeviceInfoCommand(clientFromDevice, clientToDevice, info);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForDeviceInfo(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到设备信息");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedDeviceInfo(), "服务端应成功接收设备信息");
        var receivedInfo = TestServerMessageProcessorHandler.getReceivedDeviceInfo();
        Assertions.assertNotNull(receivedInfo, "设备信息内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedInfo.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(7)
    @DisplayName("测试sendDeviceStatusCommand（字符串）")
    public void testSendDeviceStatusCommandString() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        String callId = ClientCommandSender.sendDeviceStatusCommand(clientFromDevice, clientToDevice, "ON");
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForDeviceStatus(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到设备状态");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedDeviceStatus(), "服务端应成功接收设备状态");
        var receivedStatus = TestServerMessageProcessorHandler.getReceivedDeviceStatus();
        Assertions.assertNotNull(receivedStatus, "设备状态内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedStatus.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(8)
    @DisplayName("测试sendDeviceStatusCommand（DeviceStatus）")
    public void testSendDeviceStatusCommandEntity() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(clientFromDevice.getUserId());
        status.setOnline("ON");
        String callId = ClientCommandSender.sendDeviceStatusCommand(clientFromDevice, clientToDevice, status);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForDeviceStatus(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到设备状态");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedDeviceStatus(), "服务端应成功接收设备状态");
        var receivedStatus = TestServerMessageProcessorHandler.getReceivedDeviceStatus();
        Assertions.assertNotNull(receivedStatus, "设备状态内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedStatus.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(9)
    @DisplayName("测试sendDeviceRecordCommand")
    public void testSendDeviceRecordCommand() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        DeviceRecord record = new DeviceRecord();
        record.setDeviceId(clientFromDevice.getUserId());
        String callId = ClientCommandSender.sendDeviceRecordCommand(clientFromDevice, clientToDevice, record);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForDeviceRecord(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到录像信息");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedDeviceRecord(), "服务端应成功接收录像信息");
        var receivedRecord = TestServerMessageProcessorHandler.getReceivedDeviceRecord();
        Assertions.assertNotNull(receivedRecord, "录像内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedRecord.getDeviceId(), "设备ID应一致");
    }

    @Test
    @Order(10)
    @DisplayName("测试sendDeviceConfigCommand")
    public void testSendDeviceConfigCommand() throws Exception {
        TestServerMessageProcessorHandler.resetTestState();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();
        DeviceConfigResponse config = new DeviceConfigResponse();
        config.setDeviceId(clientFromDevice.getUserId());
        String callId = ClientCommandSender.sendDeviceConfigCommand(clientFromDevice, clientToDevice, config);
        Assertions.assertNotNull(callId, "callId不能为空");
        boolean received = TestServerMessageProcessorHandler.waitForDeviceConfig(5, TimeUnit.SECONDS);
        Assertions.assertTrue(received, "服务端应收到设备配置");
        Assertions.assertTrue(TestServerMessageProcessorHandler.hasReceivedDeviceConfig(), "服务端应成功接收设备配置");
        var receivedConfig = TestServerMessageProcessorHandler.getReceivedDeviceConfig();
        Assertions.assertNotNull(receivedConfig, "设备配置内容不能为空");
        Assertions.assertEquals(clientFromDevice.getUserId(), receivedConfig.getDeviceId(), "设备ID应一致");
    }

    // 其他如sendByeCommand、sendAckCommand、sendRegisterCommand、sendUnregisterCommand等指令
    // 可根据实际服务端处理能力和断言方式补充
}