package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
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
 * GB28181 注册完整流程集成测试。
 * <p>
 * 流程: REGISTER → 401 Unauthorized → REGISTER(带认证) → 200 OK
 * <p>
 * 业务接入示范: 测试类只负责"发命令 + 断言事件回调与缓存状态",
 * SIP 监听点初始化由 {@link io.github.lunasaw.gbproxy.test.config.SipBootstrap} 完成,
 * 设备身份与密码由 {@link io.github.lunasaw.gbproxy.test.config.TestClientDeviceSupplier} /
 * {@link io.github.lunasaw.gbproxy.test.config.TestServerDeviceSupplier} 提供。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class RegistrationFlowTest {

    @Autowired
    private TestClientRegisterHandler registerHandler;

    @Autowired
    private SipBusinessConfig sessionCache;

    @Autowired
    private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}")
    private String clientId;

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
     * 完整注册流程:
     * 1. 客户端发送 REGISTER (无认证头)
     * 2. 服务端回 401 + WWW-Authenticate
     * 3. 客户端自动重发带 Authorization 的 REGISTER
     * 4. 服务端验证通过, 回 200 OK
     * 5. 断言客户端收到注册成功回调, 服务端缓存中存在该设备
     */
    @Test
    void registerFlow_shouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        registerHandler.reset(latch);

        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("注册流程应在5秒内完成").isTrue();
        assertThat(registerHandler.isRegistered()).as("客户端应收到注册成功回调").isTrue();
        assertThat(sessionCache.getToDevice(clientId)).as("服务端缓存中应存在已注册设备").isNotNull();
    }

    /**
     * 注销流程: expires=0, 服务端应将设备从缓存中移除。
     */
    @Test
    void unregisterFlow_shouldRemoveDevice() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        registerHandler.reset(latch);
        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
        latch.await(5, TimeUnit.SECONDS);
        assertThat(registerHandler.isRegistered()).isTrue();

        ClientCommandSender.sendUnregisterCommand(fromDevice, toDevice);
        Thread.sleep(500);

        assertThat(sessionCache.getToDevice(clientId)).as("注销后设备应从缓存中移除").isNull();
    }
}
