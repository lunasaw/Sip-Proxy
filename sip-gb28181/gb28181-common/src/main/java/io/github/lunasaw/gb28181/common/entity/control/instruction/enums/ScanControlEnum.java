package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 扫描指令枚举
 * 根据 A.3.6 扫描指令 规范实现
 */
@Getter
@AllArgsConstructor
public enum ScanControlEnum {

    START_AUTO_SCAN("开始自动扫描", (byte) 0x89, "开始自动扫描"),
    SET_LEFT_BOUNDARY("设置左边界", (byte) 0x89, "设置自动扫描左边界"),
    SET_RIGHT_BOUNDARY("设置右边界", (byte) 0x89, "设置自动扫描右边界"),
    SET_SCAN_SPEED("设置扫描速度", (byte) 0x8A, "设置自动扫描速度");

    /**
     * 指令名称
     */
    private final String name;

    /**
     * 指令码 (字节4)
     */
    private final byte instructionCode;

    /**
     * 功能描述
     */
    private final String description;

    // 静态映射表，用于快速查找（注意：0x89指令码对应多个枚举，通过操作类型区分）
    private static final Map<String, ScanControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ScanControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举 - 注意0x89指令码需要结合操作类型判断
     *
     * @param code 指令码
     * @return 扫描控制枚举（对于0x89返回START_AUTO_SCAN作为默认值）
     */
    public static ScanControlEnum getByCode(byte code) {
        if (code == (byte) 0x89) {
            return START_AUTO_SCAN; // 默认返回开始扫描
        } else if (code == (byte) 0x8A) {
            return SET_SCAN_SPEED;
        }
        return null;
    }

    /**
     * 根据指令码和操作类型查找枚举
     *
     * @param code          指令码
     * @param operationType 操作类型
     * @return 扫描控制枚举
     */
    public static ScanControlEnum getByCodeAndOperation(byte code, ScanOperationType operationType) {
        if (code == (byte) 0x89) {
            switch (operationType) {
                case START:
                    return START_AUTO_SCAN;
                case SET_LEFT_BOUNDARY:
                    return SET_LEFT_BOUNDARY;
                case SET_RIGHT_BOUNDARY:
                    return SET_RIGHT_BOUNDARY;
                default:
                    return START_AUTO_SCAN;
            }
        } else if (code == (byte) 0x8A) {
            return SET_SCAN_SPEED;
        }
        return null;
    }

    /**
     * 根据名称查找枚举
     */
    public static ScanControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 验证扫描组号是否有效
     *
     * @param groupNumber 扫描组号 (0-255)
     * @return 是否有效
     */
    public static boolean isValidGroupNumber(int groupNumber) {
        return groupNumber >= 0 && groupNumber <= 255;
    }

    /**
     * 验证扫描速度是否有效
     *
     * @param speed 扫描速度 (数据的低8位在字节6，高4位在字节7的高4位)
     * @return 是否有效
     */
    public static boolean isValidSpeed(int speed) {
        return speed >= 0 && speed <= 0xFFF; // 12位数据
    }

    /**
     * 扫描操作类型枚举
     */
    public enum ScanOperationType {
        START(0x00, "开始自动扫描"),
        SET_LEFT_BOUNDARY(0x01, "设置左边界"),
        SET_RIGHT_BOUNDARY(0x02, "设置右边界");

        @Getter
        private final int value;

        @Getter
        private final String description;

        ScanOperationType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public static ScanOperationType getByValue(int value) {
            for (ScanOperationType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
    }
}