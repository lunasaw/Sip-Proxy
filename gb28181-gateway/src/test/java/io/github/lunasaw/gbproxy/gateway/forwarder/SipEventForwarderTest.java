package io.github.lunasaw.gbproxy.gateway.forwarder;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
import io.github.lunasaw.gbproxy.gateway.store.InMemoryInviteContextStore;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 单元测试：SipEventForwarder 在 UDP 重传场景下幂等推送，注册/告警事件直转。
 */
class SipEventForwarderTest {

    private GatewayProperties properties;
    private InviteContextStore store;
    private BusinessNotifier notifier;
    private SipEventForwarder forwarder;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.setNodeId("node-A");
        properties.setInviteContextTtlMs(30_000);
        properties.setInviteIdempotencyWindowMs(60_000);

        store = new InMemoryInviteContextStore(30_000);
        notifier = Mockito.mock(BusinessNotifier.class);

        forwarder = new SipEventForwarder(properties, store, null, notifier);
        forwarder.initIdempotencyCache();
    }

    @Test
    void serverInvite_idempotent_perCallId() {
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", "v=0\r\n", null, "call-1_tag_1");
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", "v=0\r\n", null, "call-1_tag_1");
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", "v=0\r\n", null, "call-1_tag_1");

        verify(notifier, times(1)).inviteIncoming(eq("call-1"), eq("deviceA"), eq("deviceB"),
                eq("v=0\r\n"), any(), eq("call-1_tag_1"));

        InviteContextStore.InviteContext ctx = store.find("call-1");
        assertThat(ctx).isNotNull();
        assertThat(ctx.nodeId()).isEqualTo("node-A");
        assertThat(ctx.ctxKey()).isEqualTo("call-1_tag_1");
    }

    @Test
    void differentCallIds_routedIndependently() {
        forwarder.onServerInvite("call-A", "d1", "d2", "v=0\r\n", null, "call-A_t_1");
        forwarder.onServerInvite("call-B", "d1", "d3", "v=0\r\n", null, "call-B_t_1");

        verify(notifier, times(1)).inviteIncoming(eq("call-A"), eq("d1"), eq("d2"),
                any(), any(), eq("call-A_t_1"));
        verify(notifier, times(1)).inviteIncoming(eq("call-B"), eq("d1"), eq("d3"),
                any(), any(), eq("call-B_t_1"));
        assertThat(store.find("call-A").nodeId()).isEqualTo("node-A");
        assertThat(store.find("call-B").nodeId()).isEqualTo("node-A");
    }

    @Test
    void onServerInvite_propagates_rawSdp_and_parsed_to_notifier() {
        String rawSdp = "v=0\r\no=user 0 0 IN IP4 1.2.3.4\r\n";
        GbSessionDescription parsed = null;

        forwarder.onServerInvite("call-X", "from-1", "to-1", rawSdp, parsed, "call-X_t_1");

        verify(notifier).inviteIncoming(eq("call-X"), eq("from-1"), eq("to-1"),
                eq(rawSdp), eq(parsed), eq("call-X_t_1"));
    }

    @Test
    void onDeviceRegister_notifies_business_when_no_sessionCache() {
        RegisterInfo info = new RegisterInfo();
        info.setRemoteIp("10.0.0.1");
        info.setRemotePort(5060);
        info.setTransport("UDP");

        forwarder.onDeviceRegister("device-1", info);

        verify(notifier).deviceOnline(eq("device-1"), eq(info));
    }

    @Test
    void onAlarmNotify_passes_through_to_notifier() {
        DeviceAlarmNotify notify = new DeviceAlarmNotify();

        forwarder.onAlarmNotify("device-1", notify);

        verify(notifier).alarm(eq("device-1"), eq(notify));
    }

    @Test
    void onDeviceRegister_with_null_info_still_notifies_business() {
        forwarder.onDeviceRegister("device-1", null);

        verify(notifier).deviceOnline(eq("device-1"), eq(null));
    }
}
