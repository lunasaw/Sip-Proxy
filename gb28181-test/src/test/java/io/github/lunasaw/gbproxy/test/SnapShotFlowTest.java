package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.notify.UploadSnapShotFinishedNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientEventHandler;
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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 §9.14 图像抓拍集成测试。
 * 流程：
 *   1. server 下发 MESSAGE(Control/DeviceConfig/SnapShotConfig) → client 触发 ClientSnapShotConfigEvent
 *   2. client 上报 MESSAGE(Notify/UploadSnapShotFinished) → server 触发 DeviceSnapShotFinishedEvent
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class SnapShotFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientEventHandler clientEventHandler;
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
    void snapShotConfig_shouldReachClientAsEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        clientEventHandler.reset(latch);

        commandSender.deviceSnapShot(clientId, 3, 2, "http://example.com/upload", SESSION_ID);

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).as("图像抓拍配置应在3秒内被处理").isTrue();
        assertThat(clientEventHandler.getLastSnapShotConfig()).isNotNull();

        SnapShotConfig received = clientEventHandler.getLastSnapShotConfig().getSnapShotConfig();
        assertThat(received.getSnapShotConfig()).isNotNull();
        assertThat(received.getSnapShotConfig().getSnapNum()).isEqualTo(3);
        assertThat(received.getSnapShotConfig().getInterval()).isEqualTo(2);
        assertThat(received.getSnapShotConfig().getUploadURL()).isEqualTo("http://example.com/upload");
        assertThat(received.getSnapShotConfig().getSessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void uploadSnapShotFinishedNotify_shouldTriggerEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        ClientCommandSender.sendSnapShotFinishedNotify(fromDevice, toDevice, SESSION_ID,
                Arrays.asList("snap-001", "snap-002"));

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).as("抓拍传输完成通知应在5秒内被服务端接收").isTrue();
        UploadSnapShotFinishedNotify notify = eventHandler.getLastSnapShotFinished().getNotify();
        assertThat(notify).isNotNull();
        assertThat(notify.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(notify.getSnapShotFileIds()).containsExactly("snap-001", "snap-002");
    }
}
