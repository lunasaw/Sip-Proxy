package io.github.lunasaw.sip.common.utils;

/**
 * SM3 国密摘要算法（GM/T 0004-2012）。
 *
 * <p>GBT-28181-2022 §8.3 推荐使用 SM3 作为 SIP 信令认证的数字摘要算法。
 * 标准 JDK 不带 SM3 实现，本类按 GM/T 0004-2012 §5 直接实现，无需 BouncyCastle 依赖。
 *
 * <p>算法对外行为：
 * <ul>
 *   <li>输入：任意字节序列</li>
 *   <li>输出：32 字节（256 bit）摘要</li>
 *   <li>性能：单线程纯 Java，每秒数十 MB；REGISTER 摘要场景下毫秒内完成</li>
 * </ul>
 *
 * <p>SIP 信令场景下推荐用 {@link #digestHex(byte[])} 拿小写 hex，
 * 再用 {@link java.util.Base64} 二次编码为 §8.3 Note 头域要求的 BASE64 串。
 *
 * @author luna
 */
public final class Sm3Digest {

    private Sm3Digest() {}

    /** SM3 输出长度，单位字节。 */
    public static final int DIGEST_LENGTH = 32;

    /** GM/T 0004-2012 §4.1 IV 初始向量。 */
    private static final int[] IV = {
            0x7380166F, 0x4914B2B9, 0x172442D7, 0xDA8A0600,
            0xA96F30BC, 0x163138AA, 0xE38DEE4D, 0xB0FB0E4E
    };

    /**
     * 计算输入字节序列的 SM3 摘要。
     *
     * @param input 输入字节
     * @return 32 字节摘要
     */
    public static byte[] digest(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        byte[] padded = padding(input);
        int n = padded.length / 64;

        int[] V = IV.clone();
        for (int i = 0; i < n; i++) {
            int[] B = new int[16];
            for (int j = 0; j < 16; j++) {
                int off = i * 64 + j * 4;
                B[j] = ((padded[off] & 0xFF) << 24)
                        | ((padded[off + 1] & 0xFF) << 16)
                        | ((padded[off + 2] & 0xFF) << 8)
                        | (padded[off + 3] & 0xFF);
            }
            V = compress(V, B);
        }

        byte[] out = new byte[DIGEST_LENGTH];
        for (int i = 0; i < 8; i++) {
            out[i * 4]     = (byte) (V[i] >>> 24);
            out[i * 4 + 1] = (byte) (V[i] >>> 16);
            out[i * 4 + 2] = (byte) (V[i] >>> 8);
            out[i * 4 + 3] = (byte) V[i];
        }
        return out;
    }

    /** {@link #digest(byte[])} 的小写 hex 表达。 */
    public static String digestHex(byte[] input) {
        byte[] d = digest(input);
        StringBuilder sb = new StringBuilder(DIGEST_LENGTH * 2);
        for (byte b : d) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    // --------------------------- internal ---------------------------

    /** GM/T 0004-2012 §5.2：填充。 */
    private static byte[] padding(byte[] input) {
        int len = input.length;
        long bitLen = (long) len * 8L;
        int k = (448 - (int) ((bitLen + 1) % 512) + 512) % 512;
        int padLen = 1 + k / 8 + 8;
        byte[] padded = new byte[len + padLen];
        System.arraycopy(input, 0, padded, 0, len);
        padded[len] = (byte) 0x80;
        // bitLen 写入最末 8 字节，big-endian
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        return padded;
    }

    /** GM/T 0004-2012 §5.3：压缩函数。 */
    private static int[] compress(int[] V, int[] B) {
        int[] W = new int[68];
        int[] W1 = new int[64];

        System.arraycopy(B, 0, W, 0, 16);
        for (int j = 16; j < 68; j++) {
            W[j] = p1(W[j - 16] ^ W[j - 9] ^ rotl(W[j - 3], 15)) ^ rotl(W[j - 13], 7) ^ W[j - 6];
        }
        for (int j = 0; j < 64; j++) {
            W1[j] = W[j] ^ W[j + 4];
        }

        int A = V[0], BB = V[1], C = V[2], D = V[3];
        int E = V[4], F = V[5], G = V[6], H = V[7];

        for (int j = 0; j < 64; j++) {
            int Tj = (j < 16) ? 0x79CC4519 : 0x7A879D8A;
            int SS1 = rotl(rotl(A, 12) + E + rotl(Tj, j % 32), 7);
            int SS2 = SS1 ^ rotl(A, 12);
            int TT1 = ff(A, BB, C, j) + D + SS2 + W1[j];
            int TT2 = gg(E, F, G, j) + H + SS1 + W[j];
            D = C;
            C = rotl(BB, 9);
            BB = A;
            A = TT1;
            H = G;
            G = rotl(F, 19);
            F = E;
            E = p0(TT2);
        }

        return new int[] {
                A ^ V[0], BB ^ V[1], C ^ V[2], D ^ V[3],
                E ^ V[4], F ^ V[5], G ^ V[6], H ^ V[7]
        };
    }

    private static int rotl(int x, int n) {
        n = n & 31;
        return (x << n) | (x >>> (32 - n));
    }

    private static int ff(int x, int y, int z, int j) {
        return (j < 16) ? (x ^ y ^ z) : ((x & y) | (x & z) | (y & z));
    }

    private static int gg(int x, int y, int z, int j) {
        return (j < 16) ? (x ^ y ^ z) : ((x & y) | (~x & z));
    }

    private static int p0(int x) {
        return x ^ rotl(x, 9) ^ rotl(x, 17);
    }

    private static int p1(int x) {
        return x ^ rotl(x, 15) ^ rotl(x, 23);
    }
}
