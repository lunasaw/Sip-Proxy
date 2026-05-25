package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceUpgradeControl;
import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
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
 * GB28181-2022 §9.13 设备软件升级集成测试。
 * 流程：
 *   1. server 下发 MESSAGE(DeviceControl/DeviceUpgrade) → client 通过 ControlListener.onDeviceUpgrade 接收
 *   2. client 上报 MESSAGE(Notify/DeviceUpgradeResult) → server 触发 DeviceUpgradeResultEvent
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DeviceUpgradeFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientImpl testClient;
    @Autowired private TestServerEventHandler eventHandler;
    @Autowired private SipBusinessConfig sessionCache;
    @Autowired private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    private FromDevice fromDevice;
    private ToDevice toDevice;

    private static final String SESSION_ID = "abcdef0123456789abcdef0123456789ab";

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
    void deviceUpgradeControl_shouldDeliverFirmwareInfoToClient() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceUpgrade(clientId, "V1.0.1", "http://example.com/firmware.bin", "Manufacturer-X", SESSION_ID);

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).as("设备升级控制应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceUpgradeControl.class);

        DeviceUpgradeControl received = (DeviceUpgradeControl) testClient.getLastCommand();
        assertThat(received.getDeviceUpgrade()).isNotNull();
        assertThat(received.getDeviceUpgrade().getFirmware()).isEqualTo("V1.0.1");
        assertThat(received.getDeviceUpgrade().getFileURL()).isEqualTo("http://example.com/firmware.bin");
        assertThat(received.getDeviceUpgrade().getManufacturer()).isEqualTo("Manufacturer-X");
        assertThat(received.getDeviceUpgrade().getSessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void upgradeResultNotify_shouldTriggerEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        ClientCommandSender.sendUpgradeResultNotify(fromDevice, toDevice, SESSION_ID, "OK", "V1.0.1", null);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).as("升级结果通知应在5秒内被服务端接收").isTrue();
        UpgradeResultNotify notify = eventHandler.getLastUpgradeResult();
        assertThat(notify).isNotNull();
        assertThat(notify.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(notify.getUpgradeResult()).isEqualTo("OK");
        assertThat(notify.getFirmware()).isEqualTo("V1.0.1");
    }
}
