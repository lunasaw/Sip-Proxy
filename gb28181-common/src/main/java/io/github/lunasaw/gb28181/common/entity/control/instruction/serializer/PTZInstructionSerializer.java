package io.github.lunasaw.gb28181.common.entity.control.instruction.serializer;

import io.github.lunasaw.gb28181.common.entity.control.instruction.PTZInstructionFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * PTZ指令序列化器
 * 提供多种序列化/反序列化方式
 */
@Slf4j
public class PTZInstructionSerializer {

    /**
     * 序列化为字节数组
     *
     * @param instruction PTZ指令
     * @return 字节数组
     */
    public static byte[] serializeToBytes(PTZInstructionFormat instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("指令不能为null");
        }
        return instruction.toByteArray();
    }

    /**
     * 从字节数组反序列化
     *
     * @param bytes 字节数组
     * @return PTZ指令
     */
    public static PTZInstructionFormat deserializeFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 8) {
            throw new IllegalArgumentException("指令字节数组长度必须为8");
        }
        return PTZInstructionFormat.fromByteArray(bytes);
    }

    /**
     * 序列化为十六进制字符串
     *
     * @param instruction PTZ指令
     * @return 十六进制字符串
     */
    public static String serializeToHex(PTZInstructionFormat instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("指令不能为null");
        }
        return instruction.toHexString();
    }

    /**
     * 从十六进制字符串反序列化
     *
     * @param hexString 十六进制字符串
     * @return PTZ指令
     */
    public static PTZInstructionFormat deserializeFromHex(String hexString) {
        if (hexString == null || hexString.length() != 16) {
            throw new IllegalArgumentException("十六进制字符串长度必须为16");
        }
        return PTZInstructionFormat.fromHexString(hexString);
    }

    /**
     * 序列化为Base64字符串
     *
     * @param instruction PTZ指令
     * @return Base64字符串
     */
    public static String serializeToBase64(PTZInstructionFormat instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("指令不能为null");
        }
        byte[] bytes = instruction.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 从Base64字符串反序列化
     *
     * @param base64String Base64字符串
     * @return PTZ指令
     */
    public static PTZInstructionFormat deserializeFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Base64字符串不能为空");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            return deserializeFromBytes(bytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的Base64字符串", e);
        }
    }

    /**
     * 序列化为Java对象流
     *
     * @param instruction PTZ指令
     * @return 序列化后的字节数组
     * @throws IOException 序列化异常
     */
    public static byte[] serializeToObjectStream(PTZInstructionFormat instruction) throws IOException {
        if (instruction == null) {
            throw new IllegalArgumentException("指令不能为null");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            // 序列化指令的基本数据
            oos.writeInt(instruction.getFullAddress());
            oos.writeByte(instruction.getInstructionCode());
            oos.writeByte(instruction.getData1());
            oos.writeByte(instruction.getData2());
            oos.writeByte(instruction.getData3());
            oos.flush();

            return baos.toByteArray();
        }
    }

    /**
     * 从Java对象流反序列化
     *
     * @param bytes 序列化的字节数组
     * @return PTZ指令
     * @throws IOException            反序列化异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public static PTZInstructionFormat deserializeFromObjectStream(byte[] bytes)
            throws IOException, ClassNotFoundException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("字节数组不能为空");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            int address = ois.readInt();
            byte instructionCode = ois.readByte();
            byte data1 = ois.readByte();
            byte data2 = ois.readByte();
            byte data3 = ois.readByte();

            return new PTZInstructionFormat(address, instructionCode, data1, data2, data3);
        }
    }

    /**
     * 序列化为紧凑的字节缓冲区
     *
     * @param instruction PTZ指令
     * @return ByteBuffer
     */
    public static ByteBuffer serializeToByteBuffer(PTZInstructionFormat instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("指令不能为null");
        }

        ByteBuffer buffer = ByteBuffer.allocate(8);
        byte[] bytes = instruction.toByteArray();
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    /**
     * 从ByteBuffer反序列化
     *
     * @param buffer ByteBuffer
     * @return PTZ指令
     */
    public static PTZInstructionFormat deserializeFromByteBuffer(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() != 8) {
            throw new IllegalArgumentException("ByteBuffer剩余字节数必须为8");
        }

        byte[] bytes = new byte[8];
        buffer.get(bytes);
        return PTZInstructionFormat.fromByteArray(bytes);
    }

    /**
     * 验证序列化的完整性
     *
     * @param original     原始指令
     * @param serialized   序列化后的数据
     * @param deserializer 反序列化函数
     * @return 是否一致
     */
    public static boolean validateSerialization(PTZInstructionFormat original,
                                                Object serialized,
                                                SerializationFunction<Object, PTZInstructionFormat> deserializer) {
        try {
            PTZInstructionFormat deserialized = deserializer.apply(serialized);
            return original.equals(deserialized) && original.isValid() && deserialized.isValid();
        } catch (Exception e) {
            log.error("序列化验证失败", e);
            return false;
        }
    }

    /**
     * 序列化函数接口
     */
    @FunctionalInterface
    public interface SerializationFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * 序列化格式枚举
     */
    public enum SerializationFormat {
        BYTES("字节数组"),
        HEX("十六进制字符串"),
        BASE64("Base64字符串"),
        OBJECT_STREAM("Java对象流"),
        BYTE_BUFFER("ByteBuffer");

        private final String description;

        SerializationFormat(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}