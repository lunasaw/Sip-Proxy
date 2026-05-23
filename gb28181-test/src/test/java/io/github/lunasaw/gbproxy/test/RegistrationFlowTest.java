package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181 注册完整流程集成测试
 * 流程：REGISTER → 401 Unauthorized → REGISTER(带认证) → 200 OK
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationFlowTest {

    private static final String SERVER_ID = "34020000002000000001";
    private static final String CLIENT_ID = "34020000001320000001";
    private static final String PASSWORD   = "12345678";

    /** 测试用 ClientDeviceSupplier：getDevice(SERVER_ID) 返回带密码的 ToDevice，供 Digest 重认证使用 */
    @TestConfiguration
    static class TestDeviceSupplierConfig {
        @Bean
        @Primary
        ClientDeviceSupplier testClientDeviceSupplier() {
            return new ClientDeviceSupplier() {
                private FromDevice fromDevice = FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);

                @Override
                public Device getDevice(String userId) {
                    if (SERVER_ID.equals(userId)) {
                        ToDevice d = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
                        d.setPassword(PASSWORD);
                        return d;
                    }
                    return null;
                }

                @Override
                public FromDevice getClientFromDevice() {
                    return fromDevice;
                }

                @Override
                public void setClientFromDevice(FromDevice f) {
                    this.fromDevice = f;
                }
            };
        }
    }

    @Autowired
    private SipLayer sipLayer;

    @BeforeAll
    void initSipStack() {
        // SipLayer.sipListener 未被 Spring 自动注入，需手动设置为 CustomerSipListener 单例
        // SipProxyAutoConfig 注册处理器时也使用该单例，两者必须一致
        sipLayer.setSipListener(io.github.lunasaw.sip.common.transmit.CustomerSipListener.getInstance());
        sipLayer.addListeningPoint("127.0.0.1", 5060);
        sipLayer.addListeningPoint("127.0.0.1", 5061);
    }

    @Autowired
    private TestClientRegisterHandler registerHandler;

    @Autowired
    private SipBusinessConfig sessionCache;

    private FromDevice fromDevice;
    private ToDevice   toDevice;

    @BeforeEach
    void setUp() {
        fromDevice = FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061);
        toDevice = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        toDevice.setPassword(PASSWORD);
    }

    /**
     * 完整注册流程：
     * 1. 客户端发送 REGISTER（无认证头）
     * 2. 服务端回 401 + WWW-Authenticate
     * 3. 客户端自动重发带 Authorization 的 REGISTER
     * 4. 服务端验证通过，回 200 OK
     * 5. 断言客户端收到注册成功回调，服务端缓存中存在该设备
     */
    @Test
    void registerFlow_shouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        registerHandler.reset(latch);

        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("注册流程应在5秒内完成").isTrue();
        assertThat(registerHandler.isRegistered()).as("客户端应收到注册成功回调").isTrue();
        assertThat(sessionCache.getToDevice(CLIENT_ID)).as("服务端缓存中应存在已注册设备").isNotNull();
    }

    /**
     * 注销流程：expires=0，服务端应将设备从缓存中移除
     */
    @Test
    void unregisterFlow_shouldRemoveDevice() throws InterruptedException {
        // 先注册
        CountDownLatch latch = new CountDownLatch(1);
        registerHandler.reset(latch);
        ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
        latch.await(5, TimeUnit.SECONDS);
        assertThat(registerHandler.isRegistered()).isTrue();

        // 再注销
        ClientCommandSender.sendUnregisterCommand(fromDevice, toDevice);
        Thread.sleep(500);

        assertThat(sessionCache.getToDevice(CLIENT_ID)).as("注销后设备应从缓存中移除").isNull();
    }
}
