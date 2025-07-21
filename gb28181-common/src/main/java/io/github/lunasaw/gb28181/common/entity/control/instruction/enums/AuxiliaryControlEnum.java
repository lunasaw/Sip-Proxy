package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 辅助开关控制指令枚举
 * 根据 A.3.7 辅助开关控制指令 规范实现
 */
@Getter
@AllArgsConstructor
public enum AuxiliaryControlEnum {

    SWITCH_ON("开关开", (byte) 0x8C, "开关开 / 模拟量步进数值增加1个单位"),
    SWITCH_OFF("开关关", (byte) 0x8D, "开关关 / 模拟量步进数值减少1个单位");

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
    private static final Map<Byte, AuxiliaryControlEnum> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(AuxiliaryControlEnum::getInstructionCode, Function.identity()));

    private static final Map<String, AuxiliaryControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(AuxiliaryControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举
     */
    public static AuxiliaryControlEnum getByCode(byte code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据名称查找枚举
     */
    public static AuxiliaryControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 验证辅助开关编号是否有效
     *
     * @param switchNumber 辅助开关编号 (0-255)
     * @return 是否有效
     */
    public static boolean isValidSwitchNumber(int switchNumber) {
        return switchNumber >= 0 && switchNumber <= 255;
    }

    /**
     * 辅助开关类型枚举
     */
    public enum AuxiliarySwitchType {
        WIPER(1, "雨刷控制"),
        LIGHT(2, "灯光控制"),
        HEATING(3, "加热控制"),
        VENTILATION(4, "通风控制"),
        DEFROST(5, "除霜控制"),
        CUSTOM(0, "自定义控制");

        @Getter
        private final int value;

        @Getter
        private final String description;

        AuxiliarySwitchType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public static AuxiliarySwitchType getByValue(int value) {
            for (AuxiliarySwitchType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }
}