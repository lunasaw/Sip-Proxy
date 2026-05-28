package io.github.lunasaw.sipgateway.core.notifier;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AbstractProtocolBusinessNotifierTest {

    @Test
    void testProtocolDispatch() {
        AtomicReference<String> capturedProtocol = new AtomicReference<>();
        AbstractProtocolBusinessNotifier notifier = new AbstractProtocolBusinessNotifier() {
            @Override
            protected void onProtocolEvent(String protocol, GatewayEvent event) {
                capturedProtocol.set(protocol);
            }
        };

        notifier.notify(new GatewayEvent(
                "gb28181.Lifecycle.Online",
                "device-1", null, 0L, Map.of(), "node-1"));
        assertEquals("gb28181", capturedProtocol.get());

        notifier.notify(new GatewayEvent(
                "onvif.Discovery.Probe",
                "device-2", null, 0L, Map.of(), "node-1"));
        assertEquals("onvif", capturedProtocol.get());
    }

    @Test
    void testUnknownProtocol() {
        AtomicReference<String> capturedProtocol = new AtomicReference<>();
        AbstractProtocolBusinessNotifier notifier = new AbstractProtocolBusinessNotifier() {
            @Override
            protected void onProtocolEvent(String protocol, GatewayEvent event) {
                capturedProtocol.set(protocol);
            }
        };

        // type 不含点号 → "unknown"
        notifier.notify(new GatewayEvent(
                "Catalog", "device-1", null, 0L, Map.of(), "node-1"));
        assertEquals("unknown", capturedProtocol.get());
    }
}
