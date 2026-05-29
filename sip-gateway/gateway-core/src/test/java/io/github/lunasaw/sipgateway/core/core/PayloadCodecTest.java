package io.github.lunasaw.sipgateway.core.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayloadCodecTest {

    @Test
    void testNullReturnsNull() {
        assertNull(PayloadCodec.convert(null, String.class));
    }

    @Test
    void testNumberToInteger() {
        Integer result = PayloadCodec.convert(42L, Integer.class);
        assertEquals(42, result);
    }

    @Test
    void testNumberToLong() {
        Long result = PayloadCodec.convert(42, Long.class);
        assertEquals(42L, result);
    }

    @Test
    void testNumberToDouble() {
        Double result = PayloadCodec.convert(3.14, Double.class);
        assertEquals(3.14, result, 0.001);
    }

    @Test
    void testStringPassthrough() {
        String result = PayloadCodec.convert("hello", String.class);
        assertEquals("hello", result);
    }
}
