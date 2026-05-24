package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
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
 * GB28181 §9.5 设备信息查询集成测试。
 * 流程：server 发送 MESSAGE(Query) → client 处理并回复 MESSAGE(Response) → server 触发对应 Event。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DeviceQueryFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestServerEventHandler eventHandler;
    @Autowired private SipBusinessConfig sessionCache;
    @Autowired private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    @BeforeEach
    void ensureRegistered() throws InterruptedException {
        if (sessionCache.getToDevice(clientId) == null) {
            CountDownLatch latch = new CountDownLatch(1);
            registerHandler.reset(latch);
            var from = clientDeviceSupplier.getClientFromDevice();
            var to = (io.github.lunasaw.sip.common.entity.ToDevice) clientDeviceSupplier.getDevice(serverId);
            io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender.sendRegisterCommand(from, to, 3600);
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void catalogQuery_shouldReceiveCatalogEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        commandSender.deviceCatalogQuery(clientId);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("目录查询应在5秒内收到响应").isTrue();
        assertThat(eventHandler.getLastCatalog()).isNotNull();
        assertThat(eventHandler.getLastCatalog().getDeviceId()).isEqualTo(clientId);
        assertThat(eventHandler.getLastCatalog().getDeviceItemList()).isNotEmpty();
    }

    @Test
    void deviceInfoQuery_shouldReceiveInfoEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        commandSender.deviceInfoQuery(clientId);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("设备信息查询应在5秒内收到响应").isTrue();
        assertThat(eventHandler.getLastInfo()).isNotNull();
        assertThat(eventHandler.getLastInfo().getDeviceId()).isEqualTo(clientId);
        assertThat(eventHandler.getLastInfo().getDeviceName()).isNotBlank();
    }

    @Test
    void deviceStatusQuery_shouldReceiveStatusEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        commandSender.deviceStatusQuery(clientId);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("设备状态查询应在5秒内收到响应").isTrue();
        assertThat(eventHandler.getLastStatus()).isNotNull();
        assertThat(eventHandler.getLastStatus().getDeviceId()).isEqualTo(clientId);
        assertThat(eventHandler.getLastStatus().getOnline()).isEqualTo("ONLINE");
    }
}
