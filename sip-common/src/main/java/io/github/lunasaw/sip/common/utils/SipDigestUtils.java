package io.github.lunasaw.sip.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.util.DigestUtils;

/**
 * SIP 信令认证摘要工具。
 *
 * <p>统一提供 MD5（RFC 3261）与 SM3（GBT-28181-2022 §8.3 推荐）两种摘要算法，
 * 调用方按 {@code sip.common.signal-auth.algorithm} 配置切换。
 *
 * <p>Note 头域语义：
 * <pre>
 *   Note: Digest nonce="<base64-of-digest>", algorithm=<MD5|SM3>
 * </pre>
 *
 * @author luna
 */
public final class SipDigestUtils {

    private SipDigestUtils() {}

    /** 算法名 MD5（默认 / RFC 3261）。 */
    public static final String ALGORITHM_MD5 = "MD5";
    /** 算法名 SM3（GM/T 0004-2012 / GBT-28181-2022 §8.3）。 */
    public static final String ALGORITHM_SM3 = "SM3";

    /**
     * 按算法名计算 hex 摘要。
     *
     * @param algorithm 算法名（不区分大小写）：MD5 / SM3
     * @param input     输入字节序列
     * @return 小写 hex 摘要字符串（MD5=32 字符，SM3=64 字符）
     */
    public static String digestHex(String algorithm, byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (ALGORITHM_SM3.equalsIgnoreCase(algorithm)) {
            return Sm3Digest.digestHex(input);
        }
        // 默认 MD5（RFC 3261）
        return DigestUtils.md5DigestAsHex(input);
    }

    /**
     * 计算 §8.3 Note 头域中要求的 BASE64(摘要) nonce 值。
     *
     * @param algorithm 算法名（不区分大小写）：MD5 / SM3
     * @param input     输入字节序列
     * @return BASE64 编码后的摘要串
     */
    public static String digestBase64(String algorithm, byte[] input) {
        byte[] raw;
        if (ALGORITHM_SM3.equalsIgnoreCase(algorithm)) {
            raw = Sm3Digest.digest(input);
        } else {
            // Spring 没暴露 raw bytes 接口，自己封一下
            try {
                raw = java.security.MessageDigest.getInstance("MD5").digest(input);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("MD5 not available", e);
            }
        }
        return Base64.getEncoder().encodeToString(raw);
    }

    /**
     * 字符串便捷接口。
     */
    public static String digestHex(String algorithm, String input) {
        return digestHex(algorithm, input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字符串便捷接口（BASE64）。
     */
    public static String digestBase64(String algorithm, String input) {
        return digestBase64(algorithm, input.getBytes(StandardCharsets.UTF_8));
    }
}
