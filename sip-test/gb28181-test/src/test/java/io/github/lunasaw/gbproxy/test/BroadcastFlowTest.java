package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
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
 * GB28181-2022 §A.2.5.5 / §9.12.1 语音广播通知 (Broadcast) 端到端集成测试。
 *
 * <p>流程：server (平台) → MESSAGE(Notify, cmdType=Broadcast) → client (设备) →
 * NotifyListener.onBroadcastNotify。设备据此判断是否准备建立反向音频流。
 *
 * <p>注意：本测试只验证 Broadcast 通知通道的发送-接收路径，反向 INVITE 媒体流建立
 * 在 §9.2 InvitePlayFlowTest 已覆盖；语音对讲（§9.12.2）协议层是这两者复用，未单测。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class BroadcastFlowTest {

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
    void broadcastNotify_shouldReachClientListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceBroadcast(clientId);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("语音广播通知应 5 秒内被 onBroadcastNotify 接收（§A.2.5.5 / §9.12.1）")
            .isTrue();
        DeviceBroadcastNotify received = testClient.getLastBroadcastNotify();
        assertThat(received).isNotNull();
        assertThat(received.getCmdType()).isEqualTo(CmdTypeEnum.BROADCAST.getType());
        // SourceID = server (发起广播方) ，TargetID = client (接收并播放方)
        assertThat(received.getSourceId()).isEqualTo(serverId);
        assertThat(received.getTargetId()).isEqualTo(clientId);
    }
}
