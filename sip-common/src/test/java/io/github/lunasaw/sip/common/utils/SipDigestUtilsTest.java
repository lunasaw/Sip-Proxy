package io.github.lunasaw.sip.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SipDigestUtils} 双算法切换 + BASE64 编码验证。
 */
class SipDigestUtilsTest {

    @Test
    void digestHex_md5_shouldMatchSpringMd5() {
        String input = "user:realm:pass";
        String actual = SipDigestUtils.digestHex(SipDigestUtils.ALGORITHM_MD5, input);
        String expected = DigestUtils.md5DigestAsHex(input.getBytes());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void digestHex_sm3_shouldMatchSm3Standard() {
        String input = "abc";
        String actual = SipDigestUtils.digestHex(SipDigestUtils.ALGORITHM_SM3, input);
        // GM/T 0004-2012 §A.1
        assertThat(actual).isEqualTo(
                "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }

    @Test
    void digestHex_unknownAlgorithm_shouldFallbackToMd5() {
        String input = "fallback";
        String actual = SipDigestUtils.digestHex("UNKNOWN_ALG", input);
        String expected = DigestUtils.md5DigestAsHex(input.getBytes());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void digestBase64_md5_shouldRoundTripToRawDigest() {
        String input = "auth-seed";
        String b64 = SipDigestUtils.digestBase64(SipDigestUtils.ALGORITHM_MD5, input);
        byte[] decoded = Base64.getDecoder().decode(b64);
        assertThat(decoded).hasSize(16); // MD5 = 16 字节
    }

    @Test
    void digestBase64_sm3_shouldRoundTripTo32Bytes() {
        String b64 = SipDigestUtils.digestBase64(SipDigestUtils.ALGORITHM_SM3, "auth-seed");
        byte[] decoded = Base64.getDecoder().decode(b64);
        assertThat(decoded).hasSize(32); // SM3 = 32 字节
    }

    @Test
    void digestHex_caseInsensitiveAlgorithmName() {
        String md5 = SipDigestUtils.digestHex("md5", "x");
        String MD5 = SipDigestUtils.digestHex("MD5", "x");
        String sm3 = SipDigestUtils.digestHex("sm3", "x");
        String SM3 = SipDigestUtils.digestHex("SM3", "x");
        assertThat(md5).isEqualTo(MD5);
        assertThat(sm3).isEqualTo(SM3);
        assertThat(md5).isNotEqualTo(sm3);
    }
}
