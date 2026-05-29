package io.github.lunasaw.gbproxy.test;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.notify.VideoUploadNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigDownloadResponse;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 v1.5.6 协议补全集成测试 — 端到端验证 v1.5.5 矩阵审计发现的 5 个 dispatcher 路由缺失
 * 在补 handler 后真正能触达业务 listener。
 *
 * <p>每个测试对应矩阵 §0 中的一个原 ⚠️ 行，断言对应 listener ���法被实际回调（lat ch + getter 双重保障）。
 *
 * <ul>
 *   <li>§A.2.5.6 MobilePosition Notify：client → server，触发 onMobilePositionNotify</li>
 *   <li>§A.2.5.8 VideoUploadNotify：client → server，触发 onVideoUploadNotify</li>
 *   <li>§A.2.6.9 ConfigDownload Response：client → server，触发 onConfigDownloadResponse</li>
 *   <li>§A.2.6.10 PresetQuery Response：client → server，触发 onPresetQueryResponse</li>
 *   <li>§9.11.4 Catalog NOTIFY：client → server，触发 onNotifyUpdate</li>
 * </ul>
 *
 * <p>客户端订阅类（§A.2.4.9 MobilePosition Subscribe / §A.2.4.3 Catalog Subscribe）在
 * {@link DispatcherRegistrationTest} 中已通过注册 key 校验保障；端到端流程依赖
 * SubscribeRequestProcessor 在测试环境的设备身份校验，不在本类覆盖。
 *
 * @author luna
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class V156ProtocolCompletionFlowTest {

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
    void mobilePositionNotify_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        MobilePositionNotify notify = new MobilePositionNotify();
        notify.setCmdType("MobilePosition");
        notify.setSn(RandomStrUtil.getValidationCode());
        notify.setDeviceId(clientId);
        notify.setTime("2026-05-25T10:00:00");
        notify.setLongitude(116.397);
        notify.setLatitude(39.916);
        notify.setSpeed(60.0);

        ClientCommandSender.sendMobilePositionNotify(fromDevice, toDevice, notify);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("MobilePosition Notify 应 5 秒内被 onMobilePositionNotify 接收（v1.5.6 修复 dispatcher 路由）")
            .isTrue();
        MobilePositionNotify received = eventHandler.getLastMobilePosition();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getLongitude()).isEqualTo(116.397);
        assertThat(received.getLatitude()).isEqualTo(39.916);
    }

    @Test
    void videoUploadNotify_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        VideoUploadNotify notify = new VideoUploadNotify();
        notify.setCmdType("VideoUploadNotify");
        notify.setSn(RandomStrUtil.getValidationCode());
        notify.setDeviceId(clientId);
        notify.setTime("2026-05-25T10:00:00");
        notify.setLongitude(116.40);
        notify.setLatitude(39.90);

        ClientCommandSender.sendVideoUploadNotify(fromDevice, toDevice, notify);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("VideoUploadNotify 应 5 秒内被 onVideoUploadNotify 接收（v1.5.6 §A.2.5.8 新增）")
            .isTrue();
        VideoUploadNotify received = eventHandler.getLastVideoUpload();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getTime()).isEqualTo("2026-05-25T10:00:00");
    }

    @Test
    void configDownloadResponse_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        DeviceConfigDownloadResponse response = new DeviceConfigDownloadResponse();
        response.setSn(RandomStrUtil.getValidationCode());
        response.setDeviceId(clientId);
        response.setResult("OK");
        DeviceConfigDownloadResponse.BasicParamConfig basic = new DeviceConfigDownloadResponse.BasicParamConfig();
        basic.setName("TestCam");
        basic.setExpiration(86400);
        basic.setHeartBeatInterval(60);
        basic.setHeartBeatCount(3);
        response.setBasicParam(basic);

        ClientCommandSender.sendConfigDownloadResponse(fromDevice, toDevice, response);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("ConfigDownload Response 应 5 秒内被 onConfigDownloadResponse 接收（v1.5.6 §A.2.6.9 新增）")
            .isTrue();
        DeviceConfigDownloadResponse received = eventHandler.getLastConfigDownload();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getResult()).isEqualTo("OK");
        assertThat(received.getBasicParam()).isNotNull();
        assertThat(received.getBasicParam().getName()).isEqualTo("TestCam");
    }

    @Test
    void presetQueryResponse_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        PresetQueryResponse response = new PresetQueryResponse();
        response.setSn(RandomStrUtil.getValidationCode());
        response.setDeviceId(clientId);
        PresetQueryResponse.PresetList presetList = new PresetQueryResponse.PresetList();
        presetList.setNum(2);
        presetList.setItems(java.util.Arrays.asList(
            new PresetQueryResponse.PresetItem("1", "前门"),
            new PresetQueryResponse.PresetItem("2", "后门")));
        response.setPresetList(presetList);

        ClientCommandSender.sendPresetQueryResponse(fromDevice, toDevice, response);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("PresetQuery Response 应 5 秒内被 onPresetQueryResponse 接收（v1.5.6 §A.2.6.10 新增）")
            .isTrue();
        PresetQueryResponse received = eventHandler.getLastPresetQuery();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getPresetList()).isNotNull();
        assertThat(received.getPresetList().getItems()).hasSize(2);
        assertThat(received.getPresetList().getItems().get(0).getPresetName()).isEqualTo("前门");
    }

    @Test
    void catalogNotifyUpdate_shouldReachServerListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        // 设备主动通知通道在线/离线/属性变更（GB28181-2022 §9.11.4 / §A.2.5）
        DeviceOtherUpdateNotify.OtherItem item = new DeviceOtherUpdateNotify.OtherItem();
        item.setDeviceId(clientId);
        item.setEvent("ON");
        ClientCommandSender.sendCatalogChangeNotify(fromDevice, toDevice,
            RandomStrUtil.getValidationCode(), Collections.singletonList(item));

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("Catalog NOTIFY 应 5 秒内被 onNotifyUpdate 接收（v1.5.6 修复 CatalogNotifyHandler 注册 key）")
            .isTrue();
        DeviceOtherUpdateNotify received = eventHandler.getLastCatalogNotifyUpdate();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceItemList()).isNotEmpty();
        assertThat(received.getDeviceItemList().get(0).getEvent()).isEqualTo("ON");
    }
}
