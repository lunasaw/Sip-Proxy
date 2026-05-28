package io.github.lunasaw.gbproxy.test;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
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
 * GB28181-2022 §A.2.6.8 设备配置应答 (DeviceConfig Response，仅结果码) 端到端集成测试。
 *
 * <p>流程：client (设备) → MESSAGE(Response, cmdType=DeviceConfig, Result=OK) → server (平台) →
 * onConfigResponse listener。设备完成参数配置后回包告知执行结果。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DeviceConfigResponseFlowTest {

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
    void configResponseOk_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        DeviceConfigResponse response = new DeviceConfigResponse();
        response.setSn(RandomStrUtil.getValidationCode());
        response.setDeviceId(clientId);
        response.setResult("OK");
        ClientCommandSender.sendDeviceConfigCommand(fromDevice, toDevice, response);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("DeviceConfig Response 应 5 秒内被 onConfigResponse 接收（§A.2.6.8）")
            .isTrue();
        DeviceConfigResponse received = eventHandler.getLastConfigResponse();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getResult()).isEqualTo("OK");
    }

    @Test
    void configResponseError_shouldPassThrough() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        DeviceConfigResponse response = new DeviceConfigResponse();
        response.setSn(RandomStrUtil.getValidationCode());
        response.setDeviceId(clientId);
        response.setResult("ERROR");
        ClientCommandSender.sendDeviceConfigCommand(fromDevice, toDevice, response);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        DeviceConfigResponse received = eventHandler.getLastConfigResponse();
        assertThat(received).isNotNull();
        assertThat(received.getResult()).isEqualTo("ERROR");
    }
}
