package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
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
 * GB28181 §9.4 报警事件通知集成测试。
 * 流程：client 上报 MESSAGE(Alarm) → server 触发 DeviceAlarmEvent。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class AlarmFlowTest {

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
    void alarmNotify_shouldTriggerAlarmEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        DeviceAlarmNotify notify = new DeviceAlarmNotify();
        notify.setCmdType(CmdTypeEnum.ALARM.getType());
        notify.setSn(com.luna.common.text.RandomStrUtil.getValidationCode());
        notify.setDeviceId(clientId);
        notify.alarmMethod = "1";

        ClientCommandSender.sendAlarmCommand(fromDevice, toDevice, notify);

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("报警通知应在5秒内被服务端接收").isTrue();
        assertThat(eventHandler.getLastAlarm()).isNotNull();
        assertThat(eventHandler.getLastAlarm().getDeviceId()).isEqualTo(clientId);
        assertThat(eventHandler.getLastAlarm().alarmMethod).isEqualTo("1");
    }
}
