package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gbproxy.server.transimit.cmd.ServerSendCmd;
import io.github.lunasaw.gbproxy.test.handler.TestClientMessageProcessorHandler;
import io.github.lunasaw.gbproxy.test.handler.TestDeviceControlRequestHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

/**
 * 端到端测试：服务端DeviceControl控制命令
 * 验证服务端主动发起各类控制命令，客户端能正确接收并执行业务处理
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
public class ServerDeviceControlCommandTest extends BasicSipCommonTest {

    @Test
    @Order(1)
    @DisplayName("测试 PTZ 云台控制命令")
    public void testDeviceControlPtz() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlPtz ptzCmd = new DeviceControlPtz("DeviceControl", "1001", serverToDevice.getUserId());
        ptzCmd.setPtzCmd("A50F0100"); // 示例PTZ命令
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, ptzCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        // 补充断言
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForPtz(5, TimeUnit.SECONDS), "PTZ命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedPtz(), "PTZ命令对象未接收");
        DeviceControlPtz received = TestDeviceControlRequestHandler.getReceivedPtz();
        Assertions.assertNotNull(received, "PTZ命令对象为空");
        Assertions.assertEquals(ptzCmd.getPtzCmd(), received.getPtzCmd(), "PTZ命令内容不一致");
    }

    @Test
    @Order(2)
    @DisplayName("测试 Guard 布防/撤防命令")
    public void testDeviceControlGuard() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlGuard guardCmd = new DeviceControlGuard("DeviceControl", "1002", serverToDevice.getUserId());
        guardCmd.setGuardCmd("SetGuard");
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, guardCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForGuard(5, TimeUnit.SECONDS), "Guard命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedGuard(), "Guard命令对象未接收");
        DeviceControlGuard received = TestDeviceControlRequestHandler.getReceivedGuard();
        Assertions.assertNotNull(received, "Guard命令对象为空");
        Assertions.assertEquals(guardCmd.getGuardCmd(), received.getGuardCmd(), "Guard命令内容不一致");
    }

    @Test
    @Order(3)
    @DisplayName("测试 Alarm 告警复位命令")
    public void testDeviceControlAlarm() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlAlarm alarmCmd = new DeviceControlAlarm("DeviceControl", "1003", serverToDevice.getUserId());
        alarmCmd.setAlarmCmd("ResetAlarm");
        alarmCmd.setAlarmInfo(new DeviceControlAlarm.AlarmInfo("method", "type"));
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, alarmCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForAlarm(5, TimeUnit.SECONDS), "Alarm命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedAlarm(), "Alarm命令对象未接收");
        DeviceControlAlarm received = TestDeviceControlRequestHandler.getReceivedAlarm();
        Assertions.assertNotNull(received, "Alarm命令对象为空");
        Assertions.assertEquals(alarmCmd.getAlarmCmd(), received.getAlarmCmd(), "Alarm命令内容不一致");
        Assertions.assertNotNull(received.getAlarmInfo(), "AlarmInfo为空");
        Assertions.assertEquals(alarmCmd.getAlarmInfo().getAlarmMethod(), received.getAlarmInfo().getAlarmMethod(), "AlarmInfo method不一致");
        Assertions.assertEquals(alarmCmd.getAlarmInfo().getAlarmType(), received.getAlarmInfo().getAlarmType(), "AlarmInfo type不一致");
    }

    @Test
    @Order(4)
    @DisplayName("测试 TeleBoot 远程启动命令")
    public void testDeviceControlTeleBoot() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlTeleBoot teleBootCmd = new DeviceControlTeleBoot("DeviceControl", "1004", serverToDevice.getUserId());
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, teleBootCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForTeleBoot(5, TimeUnit.SECONDS), "TeleBoot命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedTeleBoot(), "TeleBoot命令对象未接收");
        DeviceControlTeleBoot received = TestDeviceControlRequestHandler.getReceivedTeleBoot();
        Assertions.assertNotNull(received, "TeleBoot命令对象为空");
    }

    @Test
    @Order(5)
    @DisplayName("测试 RecordCmd 录像控制命令")
    public void testDeviceControlRecordCmd() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlRecordCmd recordCmd = new DeviceControlRecordCmd("DeviceControl", "1005", serverToDevice.getUserId());
        recordCmd.setRecordCmd("Record");
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, recordCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForRecord(5, TimeUnit.SECONDS), "Record命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedRecord(), "Record命令对象未接收");
        DeviceControlRecordCmd received = TestDeviceControlRequestHandler.getReceivedRecord();
        Assertions.assertNotNull(received, "Record命令对象为空");
        Assertions.assertEquals(recordCmd.getRecordCmd(), received.getRecordCmd(), "Record命令内容不一致");
    }

    @Test
    @Order(6)
    @DisplayName("测试 IFameCmd 强制关键帧命令")
    public void testDeviceControlIFame() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlIFame iFameCmd = new DeviceControlIFame("DeviceControl", "1006", serverToDevice.getUserId());
        iFameCmd.setIFameCmd("Send");
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, iFameCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForIFame(5, TimeUnit.SECONDS), "IFame命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedIFame(), "IFame命令对象未接收");
        DeviceControlIFame received = TestDeviceControlRequestHandler.getReceivedIFame();
        Assertions.assertNotNull(received, "IFame命令对象为空");
        Assertions.assertEquals(iFameCmd.getIFameCmd(), received.getIFameCmd(), "IFame命令内容不一致");
    }

    @Test
    @Order(7)
    @DisplayName("测试 DragZoomIn 拉框放大命令")
    public void testDeviceControlDragIn() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlDragIn dragInCmd = new DeviceControlDragIn("DeviceControl", "1007", serverToDevice.getUserId());
        dragInCmd.setDragZoomIn(new DragZoom("100", "200", "50", "50", "80", "80"));
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, dragInCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForDragIn(5, TimeUnit.SECONDS), "DragIn命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedDragIn(), "DragIn命令对象未接收");
        DeviceControlDragIn received = TestDeviceControlRequestHandler.getReceivedDragIn();
        Assertions.assertNotNull(received, "DragIn命令对象为空");
        Assertions.assertNotNull(received.getDragZoomIn(), "DragZoomIn内容为空");
        Assertions.assertEquals(dragInCmd.getDragZoomIn().getLengthX(), received.getDragZoomIn().getLengthX(), "DragZoomIn X不一致");
    }

    @Test
    @Order(8)
    @DisplayName("测试 DragZoomOut 拉框缩小命令")
    public void testDeviceControlDragOut() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlDragOut dragOutCmd = new DeviceControlDragOut("DeviceControl", "1008", serverToDevice.getUserId());
        dragOutCmd.setDragZoomOut(new DragZoom("100", "200", "50", "50", "80", "80"));
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, dragOutCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForDragOut(5, TimeUnit.SECONDS), "DragOut命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedDragOut(), "DragOut命令对象未接收");
        DeviceControlDragOut received = TestDeviceControlRequestHandler.getReceivedDragOut();
        Assertions.assertNotNull(received, "DragOut命令对象为空");
        Assertions.assertNotNull(received.getDragZoomOut(), "DragZoomOut内容为空");
        Assertions.assertEquals(dragOutCmd.getDragZoomOut().getLengthX(), received.getDragZoomOut().getLengthX(), "DragZoomOut X不一致");
    }

    @Test
    @Order(9)
    @DisplayName("测试 HomePosition 看守位命令")
    public void testDeviceControlHomePosition() throws Exception {
        TestDeviceControlRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        DeviceControlPosition homePositionCmd = new DeviceControlPosition("DeviceControl", "1009", serverToDevice.getUserId());
        homePositionCmd.setHomePosition(new DeviceControlPosition.HomePosition("1", "60", "2"));
        String callId = ServerSendCmd.deviceControl(serverFromDevice, serverToDevice, homePositionCmd);
        Assertions.assertNotNull(callId, "callId不能为空");
        Assertions.assertTrue(TestDeviceControlRequestHandler.waitForHomePosition(5, TimeUnit.SECONDS), "HomePosition命令未收到");
        Assertions.assertTrue(TestDeviceControlRequestHandler.hasReceivedHomePosition(), "HomePosition命令对象未接收");
        DeviceControlPosition received = TestDeviceControlRequestHandler.getReceivedHomePosition();
        Assertions.assertNotNull(received, "HomePosition命令对象为空");
        Assertions.assertNotNull(received.getHomePosition(), "HomePosition内容为空");
        Assertions.assertEquals(homePositionCmd.getHomePosition().getPresetIndex(), received.getHomePosition().getPresetIndex(), "HomePosition PresetIndex不一致");
    }
}