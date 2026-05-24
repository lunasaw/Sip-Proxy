package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
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
 * GB28181 §9.2 实时视音频点播集成测试。
 * 流程：server 发送 INVITE → client 回复 200 OK → server 触发 DeviceInviteOkEvent → server 发送 BYE。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class InvitePlayFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
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
    void invitePlay_shouldReceiveInviteOkEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        commandSender.deviceInvitePlay(clientId, "127.0.0.1", 10000, StreamModeEnum.UDP);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("点播 INVITE 应在5秒内收到 200 OK").isTrue();
        assertThat(eventHandler.getLastInviteOk()).isNotNull();
        assertThat(eventHandler.getLastInviteOk().getDeviceId()).isEqualTo(clientId);
        assertThat(eventHandler.getLastInviteOk().getCallId()).isNotBlank();
    }

    @Test
    void invitePlay_thenBye_shouldEndSession() throws InterruptedException {
        CountDownLatch inviteLatch = new CountDownLatch(1);
        eventHandler.reset(inviteLatch);
        commandSender.deviceInvitePlay(clientId, "127.0.0.1", 10000, StreamModeEnum.UDP);
        boolean invited = inviteLatch.await(5, TimeUnit.SECONDS);
        assertThat(invited).as("点播建立应在5秒内完成").isTrue();

        String callId = eventHandler.getLastInviteOk().getCallId();
        commandSender.deviceBye(clientId, callId);
        Thread.sleep(500);
        // BYE 发送成功即可，无需等待事件（BYE 是单向的）
    }
}
