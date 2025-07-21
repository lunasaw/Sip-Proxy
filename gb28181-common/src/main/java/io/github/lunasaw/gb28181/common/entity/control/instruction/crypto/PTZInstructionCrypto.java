package io.github.lunasaw.gb28181.common.entity.control.instruction.crypto;

import io.github.lunasaw.gb28181.common.entity.control.instruction.PTZInstructionFormat;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * PTZ指令加密解密器
 * 提供多种加密算法支持
 */
@Slf4j
public class PTZInstructionCrypto {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    /**
     * 生成AES密钥
     *
     * @param keySize 密钥长度 (128, 192, 256)
     * @return AES密钥
     */
    public static SecretKey generateAESKey(int keySize) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的AES算法", e);
        }
    }

    /**
     * 从密码生成AES密钥
     *
     * @param password 密码
     * @return AES密钥
     */
    public static SecretKey generateAESKeyFromPassword(String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(password.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的SHA-256算法", e);
        }
    }

    /**
     * AES-GCM加密
     *
     * @param instruction PTZ指令
     * @param secretKey   密钥
     * @return 加密结果 (包含IV + 密文 + 认证标签)
     */
    public static byte[] encryptAESGCM(PTZInstructionFormat instruction, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密指令数据
            byte[] plaintext = instruction.toByteArray();
            byte[] ciphertext = cipher.doFinal(plaintext);

            // 组合IV和密文
            byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);

            return encryptedData;

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM加密失败", e);
        }
    }

    /**
     * AES-GCM解密
     *
     * @param encryptedData 加密数据 (包含IV + 密文 + 认证标签)
     * @param secretKey     密钥
     * @return PTZ指令
     */
    public static PTZInstructionFormat decryptAESGCM(byte[] encryptedData, SecretKey secretKey) {
        try {
            if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("加密数据长度不足");
            }

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);

            // 提取IV
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);

            // 提取密文
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密
            byte[] plaintext = cipher.doFinal(ciphertext);

            return PTZInstructionFormat.fromByteArray(plaintext);

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM解密失败", e);
        }
    }

    /**
     * 简单XOR加密 (适用于轻量级场景)
     *
     * @param instruction PTZ指令
     * @param key         密钥 (8字节)
     * @return 加密后的字节数组
     */
    public static byte[] encryptXOR(PTZInstructionFormat instruction, byte[] key) {
        if (key == null || key.length != 8) {
            throw new IllegalArgumentException("XOR密钥长度必须为8字节");
        }

        byte[] data = instruction.toByteArray();
        byte[] encrypted = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ key[i]);
        }

        return encrypted;
    }

    /**
     * 简单XOR解密
     *
     * @param encryptedData 加密数据
     * @param key           密钥 (8字节)
     * @return PTZ指令
     */
    public static PTZInstructionFormat decryptXOR(byte[] encryptedData, byte[] key) {
        if (key == null || key.length != 8) {
            throw new IllegalArgumentException("XOR密钥长度必须为8字节");
        }
        if (encryptedData == null || encryptedData.length != 8) {
            throw new IllegalArgumentException("加密数据长度必须为8字节");
        }

        byte[] decrypted = new byte[encryptedData.length];

        for (int i = 0; i < encryptedData.length; i++) {
            decrypted[i] = (byte) (encryptedData[i] ^ key[i]);
        }

        return PTZInstructionFormat.fromByteArray(decrypted);
    }

    /**
     * 计算指令数据的MD5哈希
     *
     * @param instruction PTZ指令
     * @return MD5哈希值
     */
    public static byte[] calculateMD5Hash(PTZInstructionFormat instruction) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(instruction.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的MD5算法", e);
        }
    }

    /**
     * 计算指令数据的SHA-256哈希
     *
     * @param instruction PTZ指令
     * @return SHA-256哈希值
     */
    public static byte[] calculateSHA256Hash(PTZInstructionFormat instruction) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(instruction.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的SHA-256算法", e);
        }
    }

    /**
     * 验证指令完整性
     *
     * @param instruction  PTZ指令
     * @param expectedHash 期望的哈希值
     * @param algorithm    哈希算法 ("MD5" 或 "SHA-256")
     * @return 是否一致
     */
    public static boolean verifyIntegrity(PTZInstructionFormat instruction, byte[] expectedHash, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] actualHash = digest.digest(instruction.toByteArray());
            return Arrays.equals(actualHash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("不支持的哈希算法: {}", algorithm, e);
            return false;
        }
    }

    /**
     * 生成安全的随机密钥
     *
     * @param length 密钥长度
     * @return 随机密钥
     */
    public static byte[] generateRandomKey(int length) {
        byte[] key = new byte[length];
        new SecureRandom().nextBytes(key);
        return key;
    }

    /**
     * 带认证的加密结果
     */
    public static class AuthenticatedEncryption {
        private final byte[] encryptedData;
        private final byte[] authenticationTag;

        public AuthenticatedEncryption(byte[] encryptedData, byte[] authenticationTag) {
            this.encryptedData = encryptedData;
            this.authenticationTag = authenticationTag;
        }

        public byte[] getEncryptedData() {
            return encryptedData;
        }

        public byte[] getAuthenticationTag() {
            return authenticationTag;
        }

        /**
         * 组合加密数据和认证标签
         */
        public byte[] getCombined() {
            byte[] combined = new byte[encryptedData.length + authenticationTag.length];
            System.arraycopy(encryptedData, 0, combined, 0, encryptedData.length);
            System.arraycopy(authenticationTag, 0, combined, encryptedData.length, authenticationTag.length);
            return combined;
        }
    }

    /**
     * 加密算法枚举
     */
    public enum EncryptionAlgorithm {
        AES_GCM("AES-GCM", "高安全性，带认证"),
        XOR("XOR", "轻量级，简单快速"),
        NONE("明文", "无加密");

        private final String algorithm;
        private final String description;

        EncryptionAlgorithm(String algorithm, String description) {
            this.algorithm = algorithm;
            this.description = description;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getDescription() {
            return description;
        }
    }
}