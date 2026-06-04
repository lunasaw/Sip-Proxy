package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.config.TestClientDeviceSupplier;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.transmit.DialogRegistry;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181 UDP / TCP 双协议端到端冒烟测试。
 * <p>
 * 每种 transport 依次跑：REGISTER → Catalog 查询(MESSAGE 往返) → INVITE → BYE，
 * 验证两条信令链路都能正常交互。
 * <p>
 * 关键约束：
 * <ul>
 *   <li>401 认证重发时 {@link ClientCommandSender} 从 {@link TestClientDeviceSupplier} 重新取
 *       from/to，因此切换 transport 必须作用在 supplier 上而非仅替换测试局部变量。</li>
 *   <li>服务端反向命令（Catalog / INVITE）的 transport 取自 session cache 里的 ToDevice，
 *       由 {@link SipBusinessConfig#register} 写入——已修复 transport 丢弃 bug 后才能正确携带。</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransportMatrixFlowTest {

    @Autowired private TestClientDeviceSupplier clientSupplier;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestServerEventHandler    eventHandler;
    @Autowired private SipBusinessConfig         sessionCache;
    @Autowired private ServerCommandSender       commandSender;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    // ------------------------------------------------------------------ helpers

    private void register(String transport) throws InterruptedException {
        clientSupplier.useTransport(transport);
        sessionCache.remove(clientId);

        CountDownLatch latch = new CountDownLatch(1);
        registerHandler.reset(latch);
        ClientCommandSender.sendRegisterCommand(
                clientSupplier.getClientFromDevice(),
                (io.github.lunasaw.sip.common.entity.ToDevice) clientSupplier.getDevice(serverId),
                3600);
        assertThat(latch.await(5, TimeUnit.SECONDS))
                .as("[%s] 注册应在 5 秒内完成", transport).isTrue();
        assertThat(registerHandler.isRegistered())
                .as("[%s] 客户端应收到注册成功回调", transport).isTrue();
        assertThat(sessionCache.getToDevice(clientId))
                .as("[%s] 服务端缓存应存在已注册设备", transport).isNotNull();
        assertThat(sessionCache.getToDevice(clientId).getTransport())
                .as("[%s] session cache transport 应正确保存", transport)
                .isEqualToIgnoringCase(transport);
    }

    // ------------------------------------------------------------------ tests

    /**
     * REGISTER 流程（UDP 和 TCP 各一次）。
     */
    @ParameterizedTest(name = "[{0}] 注册流程")
    @ValueSource(strings = {Constant.UDP, Constant.TCP})
    void register_shouldSucceed(String transport) throws InterruptedException {
        register(transport);
    }

    /**
     * Catalog 查询：server → MESSAGE(Query) → client 回 MESSAGE(Response)。
     * 依赖 session cache 中的 transport，验证服务端反向发送通路。
     */
    @ParameterizedTest(name = "[{0}] Catalog 查询")
    @ValueSource(strings = {Constant.UDP, Constant.TCP})
    void catalogQuery_shouldSucceed(String transport) throws InterruptedException {
        register(transport);

        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);
        commandSender.deviceCatalogQuery(clientId);

        assertThat(latch.await(5, TimeUnit.SECONDS))
                .as("[%s] Catalog 查询应在 5 秒内收到响应", transport).isTrue();
        assertThat(eventHandler.getLastCatalog()).isNotNull();
        assertThat(eventHandler.getLastCatalog().getDeviceId()).isEqualTo(clientId);
    }

    /**
     * INVITE → 200 OK → BYE → dialog 清理。
     * 验证有状态 dialog 在两种 transport 下均能正常建立与拆除。
     */
    @ParameterizedTest(name = "[{0}] INVITE/BYE 流程")
    @ValueSource(strings = {Constant.UDP, Constant.TCP})
    void inviteAndBye_shouldSucceed(String transport) throws InterruptedException {
        register(transport);

        CountDownLatch inviteLatch = new CountDownLatch(1);
        eventHandler.reset(inviteLatch);
        commandSender.deviceInvitePlay(clientId, "127.0.0.1", 10000, StreamModeEnum.UDP);

        assertThat(inviteLatch.await(5, TimeUnit.SECONDS))
                .as("[%s] INVITE 应在 5 秒内收到 200 OK", transport).isTrue();

        String callId = eventHandler.getLastInviteOkCallId();
        assertThat(callId).isNotBlank();
        assertThat(DialogRegistry.get(callId)).isNotNull();

        commandSender.deviceBye(callId);

        long deadline = System.currentTimeMillis() + 15_000L;
        while (DialogRegistry.get(callId) != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertThat(DialogRegistry.get(callId))
                .as("[%s] BYE 后 dialog 应被清理", transport).isNull();
    }
}
