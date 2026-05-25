package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlGuard;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlRecordCmd;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181 §9.3 设备控制集成测试。
 * 流程：server 发送 MESSAGE(DeviceControl) → client 收到并通过 ControlListener 派发到对应方法。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DeviceControlFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientImpl testClient;
    @Autowired private SipBusinessConfig sessionCache;
    @Autowired private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    private FromDevice fromDevice;
    private ToDevice toDevice;

    @BeforeEach
    void ensureRegistered() throws InterruptedException {
        fromDevice = clientDeviceSupplier.getClientFromDevice();
        toDevice = (ToDevice) clientDeviceSupplier.getDevice(serverId);
        if (sessionCache.getToDevice(clientId) == null) {
            CountDownLatch latch = new CountDownLatch(1);
            registerHandler.reset(latch);
            ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void ptzControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlPtzCmd(clientId, PTZControlEnum.TILT_UP, 50);

        boolean completed = latch.await(3, TimeUnit.SECONDS);

        assertThat(completed).as("PTZ 控制命令应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlPtz.class);
        DeviceControlPtz ptz = (DeviceControlPtz) testClient.getLastCommand();
        assertThat(ptz.getPtzCmd()).isNotBlank();
    }

    @Test
    void rebootControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlReboot(clientId);

        boolean completed = latch.await(3, TimeUnit.SECONDS);

        assertThat(completed).as("重启控制命令应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlTeleBoot.class);
    }

    @Test
    void recordControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlRecord(clientId, "Record");

        boolean completed = latch.await(3, TimeUnit.SECONDS);

        assertThat(completed).as("录像控制命令应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlRecordCmd.class);
        DeviceControlRecordCmd cmd = (DeviceControlRecordCmd) testClient.getLastCommand();
        assertThat(cmd.getRecordCmd()).isEqualTo("Record");
    }

    @Test
    void guardControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlGuardCmd(clientId, "SetGuard");

        boolean completed = latch.await(3, TimeUnit.SECONDS);

        assertThat(completed).as("布防控制命令应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlGuard.class);
        DeviceControlGuard cmd = (DeviceControlGuard) testClient.getLastCommand();
        assertThat(cmd.getGuardCmd()).isEqualTo("SetGuard");
    }
}
