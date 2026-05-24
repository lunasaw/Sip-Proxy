package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTargetTrack;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestDeviceControlHandler;
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
 * GB28181-2022 §9.3 / A.2.3.1.14 目标跟踪集成测试。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class TargetTrackFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestDeviceControlHandler controlHandler;
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
    void manualTargetTrack_shouldDeliverTargetArea() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        controlHandler.reset(latch);

        DeviceControlTargetTrack.TargetArea area = new DeviceControlTargetTrack.TargetArea(
                1920, 1080, 960, 540, 200, 120);
        commandSender.deviceControlTargetTrack(clientId, "Manual", "34020000001320000099", area);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("目标跟踪应在3秒内被处理").isTrue();
        assertThat(controlHandler.getLastCommand()).isInstanceOf(DeviceControlTargetTrack.class);
        DeviceControlTargetTrack received = (DeviceControlTargetTrack) controlHandler.getLastCommand();
        assertThat(received.getTargetTrack()).isEqualTo("Manual");
        assertThat(received.getDeviceId2()).isEqualTo("34020000001320000099");
        assertThat(received.getTargetArea()).isNotNull();
        assertThat(received.getTargetArea().getLength()).isEqualTo(1920);
        assertThat(received.getTargetArea().getMidPointX()).isEqualTo(960);
    }

    @Test
    void autoTargetTrack_shouldNotIncludeArea() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        controlHandler.reset(latch);

        commandSender.deviceControlTargetTrack(clientId, "Auto", null, null);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("自动跟踪应在3秒内被处理").isTrue();
        DeviceControlTargetTrack received = (DeviceControlTargetTrack) controlHandler.getLastCommand();
        assertThat(received.getTargetTrack()).isEqualTo("Auto");
        assertThat(received.getTargetArea()).isNull();
    }
}
