package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig;
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

    /** GB28181-2022 §A.2.3.2.2 基本参数配置（BasicParam）。 */
    @Test
    void basicParamConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        commandSender.deviceConfig(clientId, "TestDevice", "3600", "60", "3");

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("基本参数配置应在3秒内被处理").isTrue();
        var cfg = testClient.getLastBasicParamConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getBasicParam()).isNotNull();
        assertThat(cfg.getBasicParam().getName()).isEqualTo("TestDevice");
        assertThat(cfg.getBasicParam().getExpiration()).isEqualTo("3600");
        assertThat(cfg.getBasicParam().getHeartBeatInterval()).isEqualTo("60");
        assertThat(cfg.getBasicParam().getHeartBeatCount()).isEqualTo("3");
    }

    /** GB28181-2022 §A.2.3.2.3 SVAC 编码配置。 */
    @Test
    void svacEncodeConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        SVACEncodeConfig.SVACEncodeInfo info = new SVACEncodeConfig.SVACEncodeInfo(1, 1, 2, 3, 1, 0);
        commandSender.deviceConfigSvacEncode(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("SVAC 编码配置应在3秒内被处理").isTrue();
        SVACEncodeConfig cfg = testClient.getLastSvacEncodeConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getSvacEncodeConfig()).isNotNull();
        assertThat(cfg.getSvacEncodeConfig().getRoiEnable()).isEqualTo(1);
        assertThat(cfg.getSvacEncodeConfig().getSvcEnable()).isEqualTo(1);
        assertThat(cfg.getSvacEncodeConfig().getSvcSpaceSupportMode()).isEqualTo(2);
        assertThat(cfg.getSvacEncodeConfig().getSurveillanceOnOff()).isEqualTo(1);
    }

    /** GB28181-2022 §A.2.3.2.4 SVAC 解码配置。 */
    @Test
    void svacDecodeConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        SVACDecodeConfig.SVACDecodeInfo info = new SVACDecodeConfig.SVACDecodeInfo();
        info.setSvcSpaceSupportMode(1);
        info.setSvcTimeSupportMode(0);
        info.setSurveillanceOnOff(1);
        commandSender.deviceConfigSvacDecode(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("SVAC 解码配置应在3秒内被处理").isTrue();
        SVACDecodeConfig cfg = testClient.getLastSvacDecodeConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getSvacDecodeConfig()).isNotNull();
        assertThat(cfg.getSvacDecodeConfig().getSvcSpaceSupportMode()).isEqualTo(1);
        assertThat(cfg.getSvacDecodeConfig().getSurveillanceOnOff()).isEqualTo(1);
    }

    /** GB28181-2022 §A.2.3.2.5 视频参数属性配置。 */
    @Test
    void videoParamAttributeConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        VideoParamAttributeConfig.VideoParamAttribute info = new VideoParamAttributeConfig.VideoParamAttribute();
        info.setStreamNumber(0);
        info.setVideoFormat("H.264");
        commandSender.deviceConfigVideoParamAttribute(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("视频参数属性配置应在3秒内被处理").isTrue();
        VideoParamAttributeConfig cfg = testClient.getLastVideoParamAttributeConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getVideoParamAttribute()).isNotNull();
        assertThat(cfg.getVideoParamAttribute().getStreamNumber()).isEqualTo(0);
        assertThat(cfg.getVideoParamAttribute().getVideoFormat()).isEqualTo("H.264");
    }

    /** GB28181-2022 §A.2.3.2.x 视频参数范围配置（2016 兼容遗留）。 */
    @Test
    void videoParamOptConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        VideoParamOptConfig.VideoParamOpt info = new VideoParamOptConfig.VideoParamOpt();
        info.setDownloadSpeed("10/15/25/30");
        info.setResolution("1080P");
        commandSender.deviceConfigVideoParamOpt(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("视频参数范围配置应在3秒内被处理").isTrue();
        VideoParamOptConfig cfg = testClient.getLastVideoParamOptConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getVideoParamOpt()).isNotNull();
        assertThat(cfg.getVideoParamOpt().getDownloadSpeed()).isEqualTo("10/15/25/30");
        assertThat(cfg.getVideoParamOpt().getResolution()).isEqualTo("1080P");
    }

    /** GB28181-2022 §A.2.3.2.6 录像计划配置。 */
    @Test
    void videoRecordPlanConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        VideoRecordPlanConfig.VideoRecordPlan info = new VideoRecordPlanConfig.VideoRecordPlan();
        info.setName("DefaultPlan");
        commandSender.deviceConfigVideoRecordPlan(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("录像计划配置应在3秒内被处理").isTrue();
        VideoRecordPlanConfig cfg = testClient.getLastVideoRecordPlanConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getVideoRecordPlan()).isNotNull();
        assertThat(cfg.getVideoRecordPlan().getName()).isEqualTo("DefaultPlan");
    }

    /** GB28181-2022 §A.2.3.2.8 视频遮挡区域配置（PictureMask）。 */
    @Test
    void pictureMaskConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        PictureMaskConfig.PictureMask info = new PictureMaskConfig.PictureMask();
        info.setOn(1);
        commandSender.deviceConfigPictureMask(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("视频遮挡配置应在3秒内被处理").isTrue();
        PictureMaskConfig cfg = testClient.getLastPictureMaskConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getPictureMask()).isNotNull();
        assertThat(cfg.getPictureMask().getOn()).isEqualTo(1);
    }

    /** GB28181-2022 §A.2.3.2.9 画面镜像配置（FrameMirror）。 */
    @Test
    void frameMirrorConfig_shouldReachClientEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        testClient.reset(latch);

        FrameMirrorConfig.FrameMirror info = new FrameMirrorConfig.FrameMirror();
        info.setHorizontal(1);
        info.setVertical(0);
        commandSender.deviceConfigFrameMirror(clientId, info);

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("画面镜像配置应在3秒内被处理").isTrue();
        FrameMirrorConfig cfg = testClient.getLastFrameMirrorConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getFrameMirror()).isNotNull();
        assertThat(cfg.getFrameMirror().getHorizontal()).isEqualTo(1);
        assertThat(cfg.getFrameMirror().getVertical()).isEqualTo(0);
    }
}
