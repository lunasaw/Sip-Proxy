package io.github.lunasaw.sip.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SM3 国密摘要算法标准向量验证（GM/T 0004-2012 §A）。
 */
class Sm3DigestTest {

    /**
     * GM/T 0004-2012 §A.1：消息 "abc"。
     * 期望摘要：66c7f0f4 62eeedd9 d1f2d46b dc10e4e2 4167c487 5cf2f7a2 297da02b 8f4ba8e0
     */
    @Test
    void digestHex_shouldMatchStandardVector_abc() {
        String hex = Sm3Digest.digestHex("abc".getBytes());
        assertThat(hex).isEqualTo(
                "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }

    /**
     * GM/T 0004-2012 §A.2：消息 64 字节 "abcd" 重复 16 次（共 512 bit）。
     * 期望摘要：debe9ff9 2275b8a1 38604889 c18e5a4d 6fdb70e5 387e5765 293dcba3 9c0c5732
     */
    @Test
    void digestHex_shouldMatchStandardVector_64bytes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append("abcd");
        }
        String hex = Sm3Digest.digestHex(sb.toString().getBytes());
        assertThat(hex).isEqualTo(
                "debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732");
    }

    /**
     * 空输入边界：SM3("")。
     */
    @Test
    void digestHex_shouldHandleEmptyInput() {
        String hex = Sm3Digest.digestHex(new byte[0]);
        assertThat(hex).hasSize(64);  // 32 字节 = 64 hex 字符
        assertThat(hex).matches("[0-9a-f]{64}");
        // 已知向量：SM3("") = 1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b
        assertThat(hex).isEqualTo("1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b");
    }

    /**
     * ��出固定 32 字节。
     */
    @Test
    void digest_shouldReturn32Bytes() {
        byte[] d = Sm3Digest.digest("any input".getBytes());
        assertThat(d).hasSize(Sm3Digest.DIGEST_LENGTH);
    }

    /**
     * 同一输入应产生相同摘要（确定性）。
     */
    @Test
    void digestHex_shouldBeDeterministic() {
        String a = Sm3Digest.digestHex("test123".getBytes());
        String b = Sm3Digest.digestHex("test123".getBytes());
        assertThat(a).isEqualTo(b);
    }
}
