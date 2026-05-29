package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.DialogRegistry;
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
 * 1.7.0 新增：SUBSCRIBE 续订 / 退订集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>初始 SUBSCRIBE 后 dialog 注册到 {@link DialogRegistry}（kind=SUBSCRIBE）</li>
 *   <li>refreshSubscribe 复用同一 callId 走 dialog-aware 路径，不会触发设备 481</li>
 *   <li>unsubscribe（expires=0）后 dialog entry 被快速清理</li>
 * </ul>
 *
 * <p>本测试针对的是历史 SUBSCRIBE 续订 / 退订因不带 to-tag 触发设备 481 的协议合规问题（见
 * {@code doc/plans/1.7.0/OUTBOUND-DIALOG-PLAN.md} v1.2 §3.2.10–§3.2.14）。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class SubscribeRefreshFlowTest {

    @Autowired
    private ServerCommandSender commandSender;
    @Autowired
    private TestClientRegisterHandler registerHandler;
    @Autowired
    private TestClientImpl testClient;
    @Autowired
    private SipBusinessConfig sessionCache;
    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}")
    private String clientId;
    @Value("${sip.server.serverId}")
    private String serverId;

    @BeforeEach
    void ensureRegistered() throws InterruptedException {
        FromDevice fromDevice = clientDeviceSupplier.getClientFromDevice();
        ToDevice toDevice = (ToDevice) clientDeviceSupplier.getDevice(serverId);
        if (sessionCache.getToDevice(clientId) == null) {
            CountDownLatch latch = new CountDownLatch(1);
            registerHandler.reset(latch);
            ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    private String startAlarmSubscribe(int expires) throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        testClient.reset(clientLatch);
        String callId = commandSender.deviceAlarmSubscribe(clientId, expires, "Alarm",
                "1", "4", "2", "5",
                "2026-05-24T00:00:00", "2026-05-25T00:00:00");
        assertThat(clientLatch.await(5, TimeUnit.SECONDS))
                .as("客户端应在 5 秒内收到报警事件订阅").isTrue();
        // 等待 200 OK 回到 server，让 dialog 进入 confirmed 状态并完成 register
        Thread.sleep(300);
        return callId;
    }

    @Test
    void initialSubscribe_shouldRegisterDialog() throws InterruptedException {
        String callId = startAlarmSubscribe(3600);

        DialogRegistry.Entry entry = DialogRegistry.getEntry(callId);
        assertThat(entry).as("初始 SUBSCRIBE 后 DialogRegistry 应有 entry").isNotNull();
        assertThat(entry.getKind()).isEqualTo(DialogRegistry.KIND_SUBSCRIBE);
        assertThat(entry.getExpiresAtMs())
                .as("SUBSCRIBE entry expiresAt 应大于当前时间")
                .isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void refreshSubscribe_shouldReuseDialog() throws InterruptedException {
        String callId = startAlarmSubscribe(3600);

        // 续订 —— 走 dialog.createRequest(SUBSCRIBE)，使用同一 callId
        // 历史路径会因不带 to-tag 触发设备 481；新路径应正常完成
        String refreshedCallId = commandSender.refreshSubscribe(callId, 3600);
        assertThat(refreshedCallId).isEqualTo(callId);

        // dialog 应仍在注册表中
        assertThat(DialogRegistry.get(callId)).as("续订后 dialog 应仍在 DialogRegistry").isNotNull();
    }

    @Test
    void unsubscribe_shouldShortenExpiry() throws InterruptedException {
        String callId = startAlarmSubscribe(3600);

        // 退订
        commandSender.unsubscribe(callId);
        Thread.sleep(300);

        // 退订后 expiresAtMs 应被压缩到 60s grace 内（验证 SipSender.doSubscribeRefresh 的 register 重写逻辑）
        DialogRegistry.Entry entry = DialogRegistry.getEntry(callId);
        if (entry != null) {
            long remaining = entry.getExpiresAtMs() - System.currentTimeMillis();
            assertThat(remaining).as("unsubscribe 后 entry 应在 60s grace 内")
                    .isLessThanOrEqualTo(60_000L);
        }
        // entry 也可能已被 cleanupExpired 或 NOTIFY: terminated 清理 —— 都算正确
    }
}
