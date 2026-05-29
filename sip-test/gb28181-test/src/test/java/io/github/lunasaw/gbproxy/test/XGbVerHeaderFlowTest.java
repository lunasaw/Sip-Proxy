package io.github.lunasaw.gbproxy.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.handler.TestRegisterSuccessProbe;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GBT-28181-2022 附录 I：协议版本标识 X-GB-Ver 端到端测试。
 *
 * <p>验证：
 * <ol>
 *   <li>设备发起 REGISTER 时 OutBound 携带 {@code X-GB-Ver: 3.0}</li>
 *   <li>平台 200 OK 回包同样附带 X-GB-Ver</li>
 *   <li>平台侧 RegisterInfo.peerProtocolVersion 解析自设备 REGISTER 头</li>
 *   <li>设备侧 ClientRegisterSuccessEvent.peerProtocolVersion 解析自平台 200 OK 头</li>
 * </ol>
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class XGbVerHeaderFlowTest {

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    @Autowired
    private TestServerEventHandler serverEventHandler;

    @Autowired
    private TestRegisterSuccessProbe registerSuccessProbe;

    @Value("${sip.server.serverId}")
    private String serverId;

    private FromDevice fromDevice;
    private ToDevice   toDevice;

    @BeforeEach
    void setUp() {
        fromDevice = clientDeviceSupplier.getClientFromDevice();
        toDevice = (ToDevice) clientDeviceSupplier.getDevice(serverId);
    }

    /**
     * 端到端正向用例：注册流程双向都应携带 {@code X-GB-Ver: 3.0}。
     */
    @Test
    void registerFlow_shouldNegotiateProtocolVersion3_0() throws InterruptedException {
        CountDownLatch lifecycleLatch = new CountDownLatch(1);
        CountDownLatch clientLatch = new CountDownLatch(1);
        serverEventHandler.resetLifecycle(lifecycleLatch);
        registerSuccessProbe.reset(clientLatch);

        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);

        boolean serverGotRegister = lifecycleLatch.await(5, TimeUnit.SECONDS);
        boolean clientGotResponse = clientLatch.await(5, TimeUnit.SECONDS);

        assertThat(serverGotRegister).as("服务端应在 5s 内收到 REGISTER lifecycle").isTrue();
        assertThat(clientGotResponse).as("客户端应在 5s 内收到 200 OK").isTrue();

        // 平台收到的设备 REGISTER 中应携带 X-GB-Ver: 3.0
        assertThat(serverEventHandler.getLastRegisterInfo())
                .as("server 应解析到 RegisterInfo")
                .isNotNull();
        assertThat(serverEventHandler.getLastRegisterInfo().getPeerProtocolVersion())
                .as("REGISTER 中 X-GB-Ver 应被解析为 3.0")
                .isEqualTo("3.0");

        // 设备收到的平台 200 OK 中应携带 X-GB-Ver: 3.0
        assertThat(registerSuccessProbe.getPeerProtocolVersion())
                .as("200 OK 中 X-GB-Ver 应被解析为 3.0")
                .isEqualTo("3.0");
    }
}
