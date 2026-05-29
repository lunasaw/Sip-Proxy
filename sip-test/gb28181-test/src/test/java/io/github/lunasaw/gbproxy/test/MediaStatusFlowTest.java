package io.github.lunasaw.gbproxy.test;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
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
 * GB28181-2022 §A.2.5.4 流媒体状态通知 (MediaStatus) 端到端集成测试。
 *
 * <p>流程：client (媒体设备) → MESSAGE(Notify, cmdType=MediaStatus) → server (平台) → onMediaStatus listener。
 * 设备在媒体流结束（如历史回放结束）时主动上报 NotifyType=121。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class MediaStatusFlowTest {

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
    void mediaStatusNotify_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "121");

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("MediaStatus Notify 应 5 秒内被 onMediaStatus 接收（§A.2.5.4）")
            .isTrue();
        MediaStatusNotify received = eventHandler.getLastMediaStatus();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getNotifyType()).isEqualTo("121");
        assertThat(received.getCmdType()).isEqualTo(CmdTypeEnum.MEDIA_STATUS.getType());
    }

    /** 自定义 NotifyType 也应正常透传（121 是常用 EOF 码，但协议未限定具体值）。 */
    @Test
    void mediaStatusNotify_withCustomNotifyType_shouldPassThrough() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        MediaStatusNotify notify = new MediaStatusNotify();
        notify.setCmdType(CmdTypeEnum.MEDIA_STATUS.getType());
        notify.setSn(RandomStrUtil.getValidationCode());
        notify.setDeviceId(clientId);
        notify.setNotifyType("200");
        ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, "200");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        MediaStatusNotify received = eventHandler.getLastMediaStatus();
        assertThat(received.getNotifyType()).isEqualTo("200");
    }
}
