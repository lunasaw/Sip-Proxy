package io.github.lunasaw.sipgateway.core.api.envelope;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GatewayCommandTest {

    @Test
    void testCreate() {
        GatewayCommand cmd = new GatewayCommand(
                "gb28181.Query.Catalog",
                "34020000001320000001",
                Map.of("foo", "bar"),
                "trace-1");
        assertEquals("gb28181.Query.Catalog", cmd.type());
        assertEquals("34020000001320000001", cmd.deviceId());
        assertEquals("bar", cmd.payload().get("foo"));
        assertEquals("trace-1", cmd.requestId());
    }

    @Test
    void testWithType() {
        GatewayCommand cmd = new GatewayCommand("Query.Catalog", "id", Map.of(), null);
        GatewayCommand updated = cmd.withType("gb28181.Query.Catalog");
        assertEquals("gb28181.Query.Catalog", updated.type());
        assertEquals("id", updated.deviceId());
        // 原对象不变
        assertEquals("Query.Catalog", cmd.type());
    }
}
