package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SipEventForwarder 单元测试：UDP 重传场景下同一 callId 的 ServerInviteEvent
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
        ServerInviteEvent event = new ServerInviteEvent(this,
                "call-1", "deviceA", "deviceB",
                null, "call-1_tag_1");

        forwarder.onServerInvite(event);
        forwarder.onServerInvite(event);
        forwarder.onServerInvite(event);

        verify(notifier, times(1)).inviteIncoming(event);
        assertThat(store.find("call-1")).isEqualTo("node-A:call-1_tag_1");
    }

    @Test
    void differentCallIds_routedIndependently() {
        ServerInviteEvent e1 = new ServerInviteEvent(this,
                "call-A", "d1", "d2", null, "call-A_t_1");
        ServerInviteEvent e2 = new ServerInviteEvent(this,
                "call-B", "d1", "d3", null, "call-B_t_1");

        forwarder.onServerInvite(e1);
        forwarder.onServerInvite(e2);

        verify(notifier, times(1)).inviteIncoming(e1);
        verify(notifier, times(1)).inviteIncoming(e2);
        assertThat(store.find("call-A")).startsWith("node-A:");
        assertThat(store.find("call-B")).startsWith("node-A:");
    }
}
