package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 §A.2.5.2 心跳（Keepalive / 状态信息报送）端到端集成测试。
 *
 * <p>覆盖 4 个场景，对齐协议矩阵 §0(A) / §3.1 / §3.4：
 * <ol>
 *   <li>{@code keepaliveNotify_shouldTriggerNotifyEvent} — Client→Server 心跳上行 → DeviceNotifyListener.onKeepalive</li>
 *   <li>{@code keepaliveNotify_shouldTriggerRemoteAddressChanged} — 心跳触发 ServerLifecycleEvent.remoteAddressChanged
 *       (NAT 漂移/移动场景下平台据此更新设备寻址信息)</li>
 *   <li>{@code keepaliveFromUnregisteredDevice_shouldNotTriggerListener} — 未注册设备发心跳，server 回 404，listener 不应被调用
 *       (KeepaliveNotifyMessageHandler:53-57 分支覆盖)</li>
 *   <li>{@code clientShouldRegisterKeepaliveControlHandler} — dispatcher key 路由保护：
 *       平台→设备方向的 Control/Keepalive handler 必须注册（矩阵 §2.1 line 215；当前 ServerCommandSender
 *       未提供 sender，此测试守住协议路由不被误删）</li>
 * </ol>
 *
 * @author luna
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class KeepaliveFlowTest {

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
    @DisplayName("§A.2.5.2 已注册设备发心跳 → server DeviceNotifyListener.onKeepalive 被回调")
    void keepaliveNotify_shouldTriggerNotifyEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK");

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).as("心跳通知应在 5 秒内被服务端接收").isTrue();
        DeviceKeepLiveNotify notify = eventHandler.getLastKeepalive();
        assertThat(notify).as("DeviceNotifyListener.onKeepalive 必须收到非空 payload").isNotNull();
        assertThat(notify.getDeviceId()).isEqualTo(clientId);
        assertThat(notify.getCmdType()).isEqualTo(CmdTypeEnum.KEEPALIVE.getType());
        assertThat(notify.getStatus()).isEqualTo("OK");
    }

    @Test
    @DisplayName("§3.4 心跳触发 ServerLifecycleEvent.remoteAddressChanged（NAT 漂移路径）")
    void keepaliveNotify_shouldTriggerRemoteAddressChanged() throws InterruptedException {
        CountDownLatch lifecycleLatch = new CountDownLatch(1);
        eventHandler.resetLifecycle(lifecycleLatch);

        ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK");

        boolean completed = lifecycleLatch.await(5, TimeUnit.SECONDS);

        assertThat(completed)
            .as("KeepaliveNotifyMessageHandler:62-63 应额外发布 ServerLifecycleEvent.remoteAddressChanged")
            .isTrue();
        assertThat(eventHandler.getLastRemoteAddressInfo())
            .as("DeviceLifecycleListener.onRemoteAddressChanged 必须收到 RemoteAddressInfo（含 ip/port）")
            .isNotNull();
        assertThat(eventHandler.getLastRemoteAddressInfo().getIp())
            .as("解析自 SIP Via 头域的客户端 IP 不应为空")
            .isNotBlank();
        assertThat(eventHandler.getLastRemoteAddressInfo().getPort())
            .as("解析自 SIP Via 头域的客户端 port 必须 > 0")
            .isPositive();
    }

    @Test
    @DisplayName("§A.2.5.2 未注册设备发心跳 → server 回 404，DeviceNotifyListener.onKeepalive 不被回调（分支覆盖）")
    void keepaliveFromUnregisteredDevice_shouldNotTriggerListener() throws InterruptedException {
        // 准备：先把 clientId 从 sessionCache 摘除，模拟"未注册设备"
        sessionCache.remove(clientId);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            eventHandler.reset(latch);

            ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, "OK");

            // 期望：listener 不被调用（KeepaliveNotifyMessageHandler:53-57 在 device==null 时直接 return）
            boolean signaled = latch.await(2, TimeUnit.SECONDS);

            assertThat(signaled)
                .as("未注册设备的心跳应被 server 静默拒绝（404），DeviceNotifyListener.onKeepalive 不应被回调")
                .isFalse();
            assertThat(eventHandler.getLastKeepalive())
                .as("未注册设备的心跳不应触发 onKeepalive payload 收集")
                .isNull();
        } finally {
            // 清理：重新注册以免影响其它 @Test 顺序
            CountDownLatch reRegLatch = new CountDownLatch(1);
            registerHandler.reset(reRegLatch);
            ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
            reRegLatch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("矩阵 §2.1 client 端必须注册 Keepalive Control handler（rootType=Control, method=MESSAGE, cmdType=Keepalive）")
    void clientShouldRegisterKeepaliveControlHandler() {
        Map<String, MessageHandler> map =
            SipRequestProcessorAbstract.MESSAGE_HANDLER_CMD_MAP.get("Control");
        assertThat(map)
            .as("Control rootType 下应有 handler 注册")
            .isNotNull();
        MessageHandler handler = map.get("MESSAGE_Keepalive");
        assertThat(handler)
            .as("Keepalive Control dispatcher key 缺失：平台向设备主动下发 KeepaliveControl 时入站事件无法路由到 ControlListener.onKeepalive")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("KeepaliveMessageClientHandler");
    }
}
