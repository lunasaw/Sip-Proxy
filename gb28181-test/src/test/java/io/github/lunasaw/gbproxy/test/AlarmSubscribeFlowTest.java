package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
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
 * GB28181-2022 §9.11 报警事件订阅与通知集成测试。
 * 流程：
 *   1. server 发送 SUBSCRIBE(Alarm) → client 触发 SubscribeListener.onAlarmSubscribe
 *   2. client 通过 sendAlarmNotify 主动上报 NOTIFY(Alarm) → server 触发 DeviceAlarmEvent
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class AlarmSubscribeFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientImpl testClient;
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
    void alarmNotify_shouldReachServerEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setCmdType("Alarm");
        notify.setSn(com.luna.common.text.RandomStrUtil.getValidationCode());
        notify.setDeviceId(clientId);
        notify.setAlarmPriority("1");
        notify.setAlarmMethod("2");

        ClientCommandSender.sendAlarmNotify(fromDevice, toDevice, notify);

        assertThat(latch.await(5, TimeUnit.SECONDS)).as("报警通知应在5秒内被服务端接收").isTrue();
        assertThat(eventHandler.getLastAlarm()).isNotNull();
        assertThat(eventHandler.getLastAlarm().getNotify().getAlarmPriority()).isEqualTo("1");
        assertThat(eventHandler.getLastAlarm().getNotify().getAlarmMethod()).isEqualTo("2");
    }

    @Test
    void alarmSubscribe_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        testClient.reset(clientLatch);

        commandSender.deviceAlarmSubscribe(clientId, 3600, "Alarm",
                "1", "4", "2", "5",
                "2026-05-24T00:00:00", "2026-05-25T00:00:00");

        boolean clientCompleted = clientLatch.await(5, TimeUnit.SECONDS);
        assertThat(clientCompleted).as("客户端应在5秒内收到报警事件订阅").isTrue();
        assertThat(testClient.getLastAlarmSubscribe()).isNotNull();
        assertThat(testClient.getLastAlarmSubscribe().getDeviceId()).isEqualTo(clientId);
        assertThat(testClient.getLastAlarmSubscribe().getAlarmMethod()).isEqualTo("2");
    }
}
