package io.github.lunasaw.sipgateway.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParamBindingTest {

    @Test
    void testParseDeviceId() {
        ParamBinding b = ParamBinding.parse("deviceId");
        assertEquals("deviceId", b.source());
        assertEquals("deviceId", b.fieldName());
        assertEquals(String.class, b.targetType());
        assertNull(b.defaultValue());
    }

    @Test
    void testParseCallId() {
        ParamBinding b = ParamBinding.parse("callId");
        assertEquals("callId", b.source());
        assertEquals(String.class, b.targetType());
    }

    @Test
    void testParseStringField() {
        ParamBinding b = ParamBinding.parse("interval");
        assertEquals("payload", b.source());
        assertEquals("interval", b.fieldName());
        assertEquals(String.class, b.targetType());
        assertNull(b.defaultValue());
    }

    @Test
    void testParseIntField() {
        ParamBinding b = ParamBinding.parse("expires:int");
        assertEquals("payload", b.source());
        assertEquals("expires", b.fieldName());
        assertEquals(Integer.class, b.targetType());
        assertNull(b.defaultValue());
    }

    @Test
    void testParseIntWithDefault() {
        ParamBinding b = ParamBinding.parse("speed:int?128");
        assertEquals("payload", b.source());
        assertEquals("speed", b.fieldName());
        assertEquals(Integer.class, b.targetType());
        assertEquals(128, b.defaultValue());
    }

    @Test
    void testParseLongField() {
        ParamBinding b = ParamBinding.parse("startTime:long");
        assertEquals(Long.class, b.targetType());
    }

    @Test
    void testParseDoubleField() {
        ParamBinding b = ParamBinding.parse("pan:double");
        assertEquals(Double.class, b.targetType());
    }

    @Test
    void testUnknownTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ParamBinding.parse("foo:com.unknown.NonExistent"));
    }
}
