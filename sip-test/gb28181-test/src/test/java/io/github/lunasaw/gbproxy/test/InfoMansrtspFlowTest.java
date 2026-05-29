package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestInfoEventCapture;
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
 * GB28181-2022 §9.7-9 历史检索 / 回放 / 下载 INFO + MANSRTSP 控制端到端集成测试。
 *
 * <p>流程：server (平台) → INFO (Application/MANSRTSP, body=PLAY/PAUSE/Scale...) → client (设备) →
 * InfoRequestProcessor 自动回 200 OK + 解析 MANSRTSP body + 发布 ClientInfoEvent。
 *
 * <p>覆盖矩阵 §0(B) §9.7-9 行（PLAY 续播、PAUSE 暂停、Scale 倍速三种回放控制）。
 *
 * <p>INFO 是 L1 直消费事件（无 L2 listener），本测试通过 {@link TestInfoEventCapture} bean
 * 订阅 {@link io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent} 做断言。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class InfoMansrtspFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestInfoEventCapture eventCapture;
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

    /** §9.7 PLAY_RESUME（PAUSE 暂停回放）。 */
    @Test
    void playPause_shouldReachClientInfoEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventCapture.reset(latch);

        commandSender.deviceInvitePlayBackControl(clientId, PlayActionEnums.PLAY_RESUME);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("MANSRTSP PAUSE 应 5 秒内被 ClientInfoEvent 接收（§9.7 暂停）")
            .isTrue();
        assertThat(eventCapture.getLastContentType()).isEqualToIgnoringCase("Application/MANSRTSP");
        ManSrtspRequest parsed = eventCapture.getLastParsed();
        assertThat(parsed).isNotNull();
        assertThat(parsed.getMethod()).isEqualTo("PAUSE");
    }

    /** §9.8 PLAY_NOW（PLAY 继续/重新播放）。 */
    @Test
    void playResume_shouldReachClientInfoEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventCapture.reset(latch);

        commandSender.deviceInvitePlayBackControl(clientId, PlayActionEnums.PLAY_NOW);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("MANSRTSP PLAY 应 5 秒内被 ClientInfoEvent 接收（§9.8 继续回放）")
            .isTrue();
        ManSrtspRequest parsed = eventCapture.getLastParsed();
        assertThat(parsed).isNotNull();
        assertThat(parsed.getMethod()).isEqualTo("PLAY");
    }

    /** §9.8 PLAY_SPEED（倍速 / 倒放）。 */
    @Test
    void playSpeed_shouldReachClientInfoEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventCapture.reset(latch);

        commandSender.deviceInvitePlayBackControl(clientId, PlayActionEnums.PLAY_SPEED);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("MANSRTSP Scale 应 5 秒内被 ClientInfoEvent 接收（§9.8 倍速回放）")
            .isTrue();
        ManSrtspRequest parsed = eventCapture.getLastParsed();
        assertThat(parsed).isNotNull();
        assertThat(parsed.getMethod()).isEqualTo("PLAY");
        assertThat(parsed.getScale()).isNotNull();
        assertThat(parsed.getScale()).isEqualTo(1.0);
    }
}
