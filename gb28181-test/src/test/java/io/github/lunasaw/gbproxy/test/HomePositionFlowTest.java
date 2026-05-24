package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPosition;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
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
 * GB28181-2022 §9.5 / A.2.4.10 / A.2.6.12 / A.2.3.1.10 看守位查询 + 控制集成测试。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class HomePositionFlowTest {

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
    void homePositionControl_shouldDeliverToClient() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceControlHomePosition(clientId, "1", "60", "3");

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).as("看守位控制应在3秒内被处理").isTrue();
        assertThat(testClient.getLastCommand()).isInstanceOf(DeviceControlPosition.class);
        DeviceControlPosition received = (DeviceControlPosition) testClient.getLastCommand();
        assertThat(received.getHomePosition()).isNotNull();
        assertThat(received.getHomePosition().getEnabled()).isEqualTo("1");
        assertThat(received.getHomePosition().getResetTime()).isEqualTo("60");
        assertThat(received.getHomePosition().getPresetIndex()).isEqualTo("3");
    }

    @Test
    void homePositionQuery_shouldRoundTrip() throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        testClient.reset(clientLatch);
        CountDownLatch serverLatch = new CountDownLatch(1);
        eventHandler.reset(serverLatch);

        commandSender.deviceHomePositionQuery(clientId);

        boolean clientCompleted = clientLatch.await(5, TimeUnit.SECONDS);
        assertThat(clientCompleted).as("客户端应在5秒内收到 HomePositionQuery 查询").isTrue();
        assertThat(testClient.getLastHomePositionQuery()).isNotNull();

        boolean serverCompleted = serverLatch.await(5, TimeUnit.SECONDS);
        assertThat(serverCompleted).as("服务端应在5秒内收到看守位应答").isTrue();
        HomePositionResponse response = eventHandler.getLastHomePosition().getResponse();
        assertThat(response).isNotNull();
        assertThat(response.getHomePosition().getEnabled()).isEqualTo(1);
        assertThat(response.getHomePosition().getResetTime()).isEqualTo(60);
        assertThat(response.getHomePosition().getPresetIndex()).isEqualTo(1);
    }
}
