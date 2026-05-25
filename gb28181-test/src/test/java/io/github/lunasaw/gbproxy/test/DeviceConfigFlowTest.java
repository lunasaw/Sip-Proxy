package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientImpl;
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
 * GB28181-2022 §9.5 / A.2.3.2 设备配置扩展集成测试 (OSD / VideoAlarmRecord / AlarmReport)。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DeviceConfigFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientImpl testClient;
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
    void osdConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        OsdConfig.OsdInfo info = new OsdConfig.OsdInfo(1920, 1080, 100, 50, 1, 0, 1);
        commandSender.deviceConfigOsd(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("OSD 配置应在3秒内被处理").isTrue();
        assertThat(testClient.getLastOsdConfig()).isNotNull();
        OsdConfig osd = testClient.getLastOsdConfig();
        assertThat(osd.getOsdConfig().getLength()).isEqualTo(1920);
        assertThat(osd.getOsdConfig().getTimeX()).isEqualTo(100);
        assertThat(osd.getOsdConfig().getTimeEnable()).isEqualTo(1);
    }

    @Test
    void videoAlarmRecordConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        VideoAlarmRecordConfig.VideoAlarmRecordInfo info =
            new VideoAlarmRecordConfig.VideoAlarmRecordInfo(1, 30, 5, 0);
        commandSender.deviceConfigVideoAlarmRecord(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("报警录像配置应在3秒内被处理").isTrue();
        VideoAlarmRecordConfig cfg = testClient.getLastVideoAlarmRecordConfig();
        assertThat(cfg.getVideoAlarmRecord().getRecordEnable()).isEqualTo(1);
        assertThat(cfg.getVideoAlarmRecord().getRecordTime()).isEqualTo(30);
        assertThat(cfg.getVideoAlarmRecord().getPreRecordTime()).isEqualTo(5);
    }

    @Test
    void alarmReportConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        AlarmReportConfig.AlarmReportInfo info = new AlarmReportConfig.AlarmReportInfo(1, 0);
        commandSender.deviceConfigAlarmReport(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("报警上报配置应在3秒内被处理").isTrue();
        AlarmReportConfig cfg = testClient.getLastAlarmReportConfig();
        assertThat(cfg.getAlarmReport().getMotionDetection()).isEqualTo(1);
        assertThat(cfg.getAlarmReport().getFieldDetection()).isEqualTo(0);
    }
}
