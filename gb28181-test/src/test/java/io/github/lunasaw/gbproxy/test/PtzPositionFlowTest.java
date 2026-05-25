package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPTZPrecise;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
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
 * GB28181-2022 §9.5 / A.2.3.1.11 / A.2.4.13 / A.2.6.15 PTZ 精准控制 + 精确状态查询集成测试。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class PtzPositionFlowTest {

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
    void ptzPreciseControl_shouldInvokeHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlPtzPrecise(clientId, 180.5, 30.25, 2.0);

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).as("PTZ 精准控制应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlPTZPrecise.class);
        DeviceControlPTZPrecise received = (DeviceControlPTZPrecise) testClient.getLastCommand();
        assertThat(received.getPtzPreciseCtrl()).isNotNull();
        assertThat(received.getPtzPreciseCtrl().getPan()).isEqualTo(180.5);
        assertThat(received.getPtzPreciseCtrl().getTilt()).isEqualTo(30.25);
        assertThat(received.getPtzPreciseCtrl().getZoom()).isEqualTo(2.0);
    }

    @Test
    void ptzPositionQuery_shouldRoundTrip() throws InterruptedException {
        // 客户端在 TestClientImpl 中自动回包，本测试同时验证 server 收到 PTZPosition 应答
        CountDownLatch clientLatch = new CountDownLatch(1);
        testClient.reset(clientLatch);
        CountDownLatch serverLatch = new CountDownLatch(1);
        eventHandler.reset(serverLatch);

        commandSender.devicePtzPositionQuery(clientId);

        boolean clientCompleted = clientLatch.await(5, TimeUnit.SECONDS);
        assertThat(clientCompleted).as("客户端应在5秒内收到 PTZPosition 查询并发出应答").isTrue();
        assertThat(testClient.getLastPtzPositionQuery()).isNotNull();
        assertThat(testClient.getLastPtzPositionQuery().getDeviceId()).isEqualTo(clientId);

        boolean serverCompleted = serverLatch.await(5, TimeUnit.SECONDS);
        assertThat(serverCompleted).as("服务端应在5秒内收到 PTZPosition 应答").isTrue();
        PTZPositionResponse response = eventHandler.getLastPtzPosition();
        assertThat(response).isNotNull();
        assertThat(response.getPan()).isEqualTo(180.0);
        assertThat(response.getTilt()).isEqualTo(30.0);
        assertThat(response.getZoom()).isEqualTo(2.0);
    }
}
