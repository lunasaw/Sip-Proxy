package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FI(聚焦Focus/光圈Iris)控制指令枚举
 * 根据 A.3.3 FI指令 规范实现
 * <p>
 * 字节4位定义:
 * Bit7-Bit6: 固定为01
 * Bit5-Bit4: 固定为00
 * Bit3-Bit2: 光圈控制 (Iris)
 * Bit1-Bit0: 聚焦控制 (Focus)
 */
@Getter
@AllArgsConstructor
public enum FIControlEnum {

    // 基础控制
    STOP("停止", (byte) 0x40, "镜头停止FI的所有动作"),

    // 光圈控制
    IRIS_CLOSE("光圈缩小", (byte) 0x48, "镜头光圈缩小"),
    IRIS_OPEN("光圈放大", (byte) 0x44, "镜头光圈放大"),

    // 聚焦控制
    FOCUS_NEAR("聚焦近", (byte) 0x42, "镜头聚焦近"),
    FOCUS_FAR("聚焦远", (byte) 0x41, "镜头聚焦远"),

    // 组合控制示例
    IRIS_CLOSE_FOCUS_FAR("光圈缩小聚焦远", (byte) 0x49, "镜头光圈缩小同时聚焦远");

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

    // 静态映射表，用于快速查找
    private static final Map<Byte, FIControlEnum> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(FIControlEnum::getInstructionCode, Function.identity()));

    private static final Map<String, FIControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(FIControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举
     */
    public static FIControlEnum getByCode(byte code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据名称查找枚举
     */
    public static FIControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 检查是否包含光圈控制
     */
    public boolean hasIrisControl() {
        return (instructionCode & 0x0C) != 0;
    }

    /**
     * 检查是否包含聚焦控制
     */
    public boolean hasFocusControl() {
        return (instructionCode & 0x03) != 0;
    }

    /**
     * 获取光圈控制类型
     */
    public IrisDirection getIrisDirection() {
        byte irisBits = (byte) (instructionCode & 0x0C);
        switch (irisBits) {
            case 0x04:
                return IrisDirection.OPEN;
            case 0x08:
                return IrisDirection.CLOSE;
            default:
                return IrisDirection.NONE;
        }
    }

    /**
     * 获取聚焦控制类型
     */
    public FocusDirection getFocusDirection() {
        byte focusBits = (byte) (instructionCode & 0x03);
        switch (focusBits) {
            case 0x01:
                return FocusDirection.FAR;
            case 0x02:
                return FocusDirection.NEAR;
            default:
                return FocusDirection.NONE;
        }
    }

    /**
     * 光圈方向枚举
     */
    public enum IrisDirection {
        NONE("无"), OPEN("放大"), CLOSE("缩小");

        @Getter
        private final String name;

        IrisDirection(String name) {
            this.name = name;
        }
    }

    /**
     * 聚焦方向枚举
     */
    public enum FocusDirection {
        NONE("无"), NEAR("近"), FAR("远");

        @Getter
        private final String name;

        FocusDirection(String name) {
            this.name = name;
        }
    }
}