package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.instruction.PTZInstructionFormat;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.AuxiliaryControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PresetControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GBT-28181-2022 §A.3 前端设备控制协议端到端集成测试。
 *
 * <p>验证 v1.6.0 新增的 5 类高层 sender（FI / Preset / Cruise / Scan / Auxiliary）+ 既有 PTZ 入口
 * 把 8 字节二进制指令正确编码进 {@code <PTZCmd>} hex 串，client 收到后 hex 串通过
 * {@link PTZInstructionFormat#isValid()} 校验。
 *
 * @author luna
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class FrontEndControlFlowTest {

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
    @DisplayName("§A.3.3 deviceControlFI（光圈缩小 + 聚焦远）→ client 收到 PTZCmd 字节 4=49H")
    void deviceControlFI_irisCloseFocusFar() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlFI(clientId,
            FIControlEnum.IrisDirection.CLOSE,
            FIControlEnum.FocusDirection.FAR,
            /*focusSpeed=*/ 100, /*irisSpeed=*/ 80);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("FI 控制应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        String hex = received.getPtzCmd();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(hex);
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x49);  // 0x40 | 0x08 | 0x01
    }

    @Test
    @DisplayName("§A.3.4 deviceControlPreset（调用预置位 5）→ client 收到字节 4=82H, 字节 6=05H")
    void deviceControlPreset_callPreset5() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlPreset(clientId, PresetControlEnum.CALL_PRESET, 5);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("Preset 控制应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(received.getPtzCmd());
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x82);
        assertThat(parsed.getData2() & 0xFF).isEqualTo(5);
    }

    @Test
    @DisplayName("§A.3.5 deviceControlCruise（加入巡航点 组 1 预置位 5）→ 字节 4=84H, 字节 5=01H, 字节 6=05H")
    void deviceControlCruise_addPoint() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlCruise(clientId,
            CruiseControlEnum.ADD_CRUISE_POINT, 1, 5);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("Cruise 加入巡航点应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(received.getPtzCmd());
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x84);
        assertThat(parsed.getData1() & 0xFF).isEqualTo(1);
        assertThat(parsed.getData2() & 0xFF).isEqualTo(5);
    }

    @Test
    @DisplayName("§A.3.6 deviceControlScan（开始自动扫描 组 1）→ 字节 4=89H, 字节 5=01H, 字节 6=00H")
    void deviceControlScan_startAuto() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlScan(clientId, 1, ScanControlEnum.ScanOperationType.START);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("Scan 开始扫描应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(received.getPtzCmd());
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x89);
        assertThat(parsed.getData1() & 0xFF).isEqualTo(1);
        assertThat(parsed.getData2() & 0xFF).isEqualTo(0);
    }

    @Test
    @DisplayName("§A.3.6 deviceControlScanSpeed（组 1 速度 50）→ 字节 4=8AH, 字节 5=01H, 字节 6=32H")
    void deviceControlScanSpeed_setSpeed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlScanSpeed(clientId, 1, 50);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("Scan 速度设置应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(received.getPtzCmd());
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x8A);
        assertThat(parsed.getData2() & 0xFF).isEqualTo(50);
    }

    @Test
    @DisplayName("§A.3.7 deviceControlAuxiliary（雨刷开 switch=1）→ 字节 4=8CH, 字节 5=01H")
    void deviceControlAuxiliary_wiperOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlAuxiliary(clientId, AuxiliaryControlEnum.SWITCH_ON, 1);

        assertThat(latch.await(3, TimeUnit.SECONDS))
            .as("Auxiliary 控制应 3 秒内触达 client").isTrue();
        DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(received.getPtzCmd());
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getInstructionCode() & 0xFF).isEqualTo(0x8C);
        assertThat(parsed.getData1() & 0xFF).isEqualTo(1);
    }
}
