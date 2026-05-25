package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragIn;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragOut;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlIFame;
import io.github.lunasaw.gb28181.common.entity.control.DragZoom;
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
 * GB28181-2022 §9.3 / A.2.3.1.7 / A.2.3.1.8 / A.2.3.1.9 强制关键帧 + 拉框放大/缩小集成测试。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class IFrameDragFlowTest {

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
    void iframeControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlIFrame(clientId);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("强制关键帧应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlIFame.class);
        DeviceControlIFame received = (DeviceControlIFame) testClient.getLastCommand();
        assertThat(received.getIFameCmd()).isEqualTo("Send");
    }

    @Test
    void dragZoomIn_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        DragZoom zoom = new DragZoom("1920", "1080", "960", "540", "200", "120");
        commandSender.deviceControlDragZoomIn(clientId, zoom);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("拉框放大应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlDragIn.class);
        DeviceControlDragIn received = (DeviceControlDragIn) testClient.getLastCommand();
        assertThat(received.getDragZoomIn().getLength()).isEqualTo("1920");
        assertThat(received.getDragZoomIn().getMidPointX()).isEqualTo("960");
    }

    @Test
    void dragZoomOut_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        DragZoom zoom = new DragZoom("1920", "1080", "100", "100", "50", "30");
        commandSender.deviceControlDragZoomOut(clientId, zoom);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("拉框缩小应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlDragOut.class);
        DeviceControlDragOut received = (DeviceControlDragOut) testClient.getLastCommand();
        assertThat(received.getDragZoomOut().getLength()).isEqualTo("1920");
    }
}
