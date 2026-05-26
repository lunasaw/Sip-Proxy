package io.github.lunasaw.gbproxy.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.handler.TestRegisterSuccessProbe;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GBT-28181-2022 §8.3：SIP 信令认证扩展端到端测试。
 *
 * <p>开启 {@code sip.common.signal-auth.enabled=true} 后，REGISTER 请求应携带：
 * <ul>
 *   <li>{@code Note: Digest nonce="<base64>", algorithm=SM3}</li>
 *   <li>{@code Monitor-User-Identity: gw-user-attr}（同域发起场景）</li>
 * </ul>
 *
 * <p>用例运行时通过 {@link SipCommonProperties.SignalAuth} 直接 enable/disable，
 * 避免 Spring {@code properties=} 引起的多 Context 端口冲突。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class SignalAuthHeaderFlowTest {

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    @Autowired
    private TestServerEventHandler serverEventHandler;

    @Autowired
    private TestRegisterSuccessProbe registerSuccessProbe;

    @Autowired
    private SipCommonProperties sipCommonProperties;

    @Value("${sip.server.serverId}")
    private String serverId;

    private FromDevice fromDevice;
    private ToDevice   toDevice;

    @BeforeEach
    void setUp() {
        fromDevice = clientDeviceSupplier.getClientFromDevice();
        toDevice = (ToDevice) clientDeviceSupplier.getDevice(serverId);

        // 临时启用 §8.3 信令认证扩展（用例结束后恢复）
        SipCommonProperties.SignalAuth cfg = sipCommonProperties.getSignalAuth();
        cfg.setEnabled(true);
        cfg.setAlgorithm("SM3");
        cfg.setGatewayId("gw-test-001");
        cfg.setUserId("user-flow-001");
        cfg.setUserAttribute("role-test");
    }

    @AfterEach
    void tearDown() {
        // 恢复默认状态，避免污染共享 Context 中的兄弟测试
        SipCommonProperties.SignalAuth cfg = sipCommonProperties.getSignalAuth();
        cfg.setEnabled(false);
        cfg.setAlgorithm("MD5");
        cfg.setGatewayId(null);
        cfg.setUserId(null);
        cfg.setUserAttribute(null);
    }

    @Test
    void registerFlow_shouldCarryNoteAndMonitorUserIdentity() throws InterruptedException {
        CountDownLatch lifecycleLatch = new CountDownLatch(1);
        CountDownLatch clientLatch = new CountDownLatch(1);
        serverEventHandler.resetLifecycle(lifecycleLatch);
        registerSuccessProbe.reset(clientLatch);

        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);

        boolean serverGotRegister = lifecycleLatch.await(5, TimeUnit.SECONDS);
        boolean clientGotResponse = clientLatch.await(5, TimeUnit.SECONDS);

        assertThat(serverGotRegister).as("服务端应在 5s 内收到 REGISTER lifecycle").isTrue();
        assertThat(clientGotResponse).as("客户端应在 5s 内收到 200 OK").isTrue();

        assertThat(serverEventHandler.getLastRegisterInfo())
                .as("server 应解析到 RegisterInfo")
                .isNotNull();

        // §8.3 Note：应携带 SM3 算法标识 + 非空 BASE64 摘要
        String note = serverEventHandler.getLastRegisterInfo().getPeerNote();
        assertThat(note).as("Note 头应被注入").isNotNull();
        assertThat(note).as("algorithm 应为 SM3").contains("algorithm=SM3");
        assertThat(note).as("应包含 Digest 关键字与 nonce 字段").contains("Digest nonce=\"");

        // §8.3 Monitor-User-Identity：同域发起 = gateway-id - user-id - user-attribute
        String mui = serverEventHandler.getLastRegisterInfo().getPeerMonitorUserIdentity();
        assertThat(mui).as("Monitor-User-Identity 头应被注入").isNotNull();
        assertThat(mui).isEqualTo("gw-test-001-user-flow-001-role-test");
    }
}
