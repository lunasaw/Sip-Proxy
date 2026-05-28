package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PTZ控制指令枚举
 * 根据 A.3.2 PTZ指令 规范实现
 * <p>
 * 字节4位定义:
 * Bit7-Bit6: 固定为00
 * Bit5-Bit4: 镜头变倍控制 (Zoom)
 * Bit3-Bit2: 云台垂直方向控制 (Tilt)
 * Bit1-Bit0: 云台水平方向控制 (Pan)
 */
@Getter
@AllArgsConstructor
public enum PTZControlEnum {

    // 基础PTZ控制
    STOP("停止", (byte) 0x00, "PTZ的所有操作均停止"),

    // 水平方向控制
    PAN_LEFT("向左", (byte) 0x02, "云台向左方向运动"),
    PAN_RIGHT("向右", (byte) 0x01, "云台向右方向运动"),

    // 垂直方向控制  
    TILT_UP("向上", (byte) 0x08, "云台向上方向运动"),
    TILT_DOWN("向下", (byte) 0x04, "云台向下方向运动"),

    // 组合方向控制
    PAN_LEFT_TILT_UP("左上", (byte) 0x0A, "云台向左上方向运动"),
    PAN_RIGHT_TILT_UP("右上", (byte) 0x09, "云台向右上方向运动"),
    PAN_LEFT_TILT_DOWN("左下", (byte) 0x06, "云台向左下方向运动"),
    PAN_RIGHT_TILT_DOWN("右下", (byte) 0x05, "云台向右下方向运动"),

    // 镜头变倍控制
    ZOOM_IN("放大", (byte) 0x10, "镜头变倍放大"),
    ZOOM_OUT("缩小", (byte) 0x20, "镜头变倍缩小"),

    // 组合控制示例 (可扩展)
    PAN_RIGHT_TILT_UP_ZOOM_OUT("右上缩小", (byte) 0x29, "云台右上运动同时镜头缩小");

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
    private static final Map<Byte, PTZControlEnum> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PTZControlEnum::getInstructionCode, Function.identity()));

    private static final Map<String, PTZControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PTZControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举
     */
    public static PTZControlEnum getByCode(byte code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据名称查找枚举
     */
    public static PTZControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 检查是否包含水平方向控制
     */
    public boolean hasPanControl() {
        return (instructionCode & 0x03) != 0;
    }

    /**
     * 检查是否包含垂直方向控制
     */
    public boolean hasTiltControl() {
        return (instructionCode & 0x0C) != 0;
    }

    /**
     * 检查是否包含变倍控制
     */
    public boolean hasZoomControl() {
        return (instructionCode & 0x30) != 0;
    }

    /**
     * 获取水平方向控制类型
     */
    public PanDirection getPanDirection() {
        byte panBits = (byte) (instructionCode & 0x03);
        switch (panBits) {
            case 0x01:
                return PanDirection.RIGHT;
            case 0x02:
                return PanDirection.LEFT;
            default:
                return PanDirection.NONE;
        }
    }

    /**
     * 获取垂直方向控制类型
     */
    public TiltDirection getTiltDirection() {
        byte tiltBits = (byte) (instructionCode & 0x0C);
        switch (tiltBits) {
            case 0x04:
                return TiltDirection.DOWN;
            case 0x08:
                return TiltDirection.UP;
            default:
                return TiltDirection.NONE;
        }
    }

    /**
     * 获取变倍控制类型
     */
    public ZoomDirection getZoomDirection() {
        byte zoomBits = (byte) (instructionCode & 0x30);
        switch (zoomBits) {
            case 0x10:
                return ZoomDirection.IN;
            case 0x20:
                return ZoomDirection.OUT;
            default:
                return ZoomDirection.NONE;
        }
    }

    /**
     * 水平方向枚举
     */
    public enum PanDirection {
        NONE("无"), LEFT("左"), RIGHT("右");

        @Getter
        private final String name;

        PanDirection(String name) {
            this.name = name;
        }
    }

    /**
     * 垂直方向枚举
     */
    public enum TiltDirection {
        NONE("无"), UP("上"), DOWN("下");

        @Getter
        private final String name;

        TiltDirection(String name) {
            this.name = name;
        }
    }

    /**
     * 变倍方向枚举
     */
    public enum ZoomDirection {
        NONE("无"), IN("放大"), OUT("缩小");

        @Getter
        private final String name;

        ZoomDirection(String name) {
            this.name = name;
        }
    }
}