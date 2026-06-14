package io.github.lunasaw.gbproxy.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;

/**
 * GB/T 28181-2022 §9.11.3 目录订阅集成测试。
 *
 * <p>对应根因修复：目录订阅 {@code deviceCatalogSubscribe} 旧实现从不设置 eventId，
 * 导致 SUBSCRIBE 构建时 Event 头 {@code setEventId(null)} 抛 NPE，SUBSCRIBE 根本发不出去。
 * 修复后（N.4.2：Event 头域必须携带数字 id）SUBSCRIBE 可正常下发，client 触发
 * {@code SubscribeListener.onCatalogSubscribe}。</p>
 *
 * @author luna
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class CatalogSubscribeFlowTest {

    @Autowired
    private ServerCommandSender       commandSender;
    @Autowired
    private TestClientRegisterHandler registerHandler;
    @Autowired
    private TestClientImpl            testClient;
    @Autowired
    private SipBusinessConfig         sessionCache;
    @Autowired
    private ClientDeviceSupplier      clientDeviceSupplier;

    @Value("${sip.client.clientId}")
    private String                    clientId;
    @Value("${sip.server.serverId}")
    private String                    serverId;

    private FromDevice                fromDevice;
    private ToDevice                  toDevice;

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
    @DisplayName("目录订阅 SUBSCRIBE 应成功下发并到达 client（修复前因 eventId=null 抛 NPE）")
    void catalogSubscribe_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        testClient.reset(clientLatch);

        commandSender.deviceCatalogSubscribe(clientId, 3600, "Catalog");

        boolean clientCompleted = clientLatch.await(5, TimeUnit.SECONDS);
        assertThat(clientCompleted).as("客户端应在5秒内收到目录订阅").isTrue();
        assertThat(testClient.getLastCatalogSubscribe()).isNotNull();
        assertThat(testClient.getLastCatalogSubscribe().getDeviceId()).isEqualTo(clientId);
    }
}
