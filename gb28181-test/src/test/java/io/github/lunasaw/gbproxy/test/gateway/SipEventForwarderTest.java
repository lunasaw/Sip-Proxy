package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SipEventForwarder 单元测试：UDP 重传场景下同一 callId 的 server INVITE 事件
 * 必须只触发一次业务推送，且 InviteContextStore 写入幂等。
 */
class SipEventForwarderTest {

    private GatewayProperties properties;
    private InviteContextStore store;
    private SipBusinessConfig sessionCache;
    private BusinessNotifier notifier;
    private SipEventForwarder forwarder;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.setNodeId("node-A");
        properties.setInviteContextTtlMs(30_000);
        properties.setInviteIdempotencyWindowMs(60_000);

        store = new InMemoryInviteContextStore(30_000);
        sessionCache = new SipBusinessConfig();
        notifier = Mockito.mock(BusinessNotifier.class);

        forwarder = new SipEventForwarder(properties, store, sessionCache, notifier);
        forwarder.initIdempotencyCache();
    }

    @Test
    void serverInvite_idempotent_perCallId() {
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", null, "call-1_tag_1");
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", null, "call-1_tag_1");
        forwarder.onServerInvite("call-1", "deviceA", "deviceB", null, "call-1_tag_1");

        verify(notifier, times(1)).inviteIncoming(eq("call-1"), eq("deviceA"), eq("deviceB"), any(), eq("call-1_tag_1"));
        assertThat(store.find("call-1")).isEqualTo("node-A:call-1_tag_1");
    }

    @Test
    void differentCallIds_routedIndependently() {
        forwarder.onServerInvite("call-A", "d1", "d2", null, "call-A_t_1");
        forwarder.onServerInvite("call-B", "d1", "d3", null, "call-B_t_1");

        verify(notifier, times(1)).inviteIncoming(eq("call-A"), eq("d1"), eq("d2"), any(), eq("call-A_t_1"));
        verify(notifier, times(1)).inviteIncoming(eq("call-B"), eq("d1"), eq("d3"), any(), eq("call-B_t_1"));
        assertThat(store.find("call-A")).startsWith("node-A:");
        assertThat(store.find("call-B")).startsWith("node-A:");
    }
}
