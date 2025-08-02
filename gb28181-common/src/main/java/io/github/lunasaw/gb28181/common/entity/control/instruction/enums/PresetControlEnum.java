package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预置位指令枚举
 * 根据 A.3.4 预置位指令 规范实现
 * <p>
 * 预置位数目最大为255，0号预留
 */
@Getter
@AllArgsConstructor
public enum PresetControlEnum {

    SET_PRESET("设置预置位", (byte) 0x81, "设置预置位"),
    CALL_PRESET("调用预置位", (byte) 0x82, "调用预置位"),
    DELETE_PRESET("删除预置位", (byte) 0x83, "删除预置位");

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
    private static final Map<Byte, PresetControlEnum> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PresetControlEnum::getInstructionCode, Function.identity()));

    private static final Map<String, PresetControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PresetControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举
     */
    public static PresetControlEnum getByCode(byte code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据名称查找枚举
     */
    public static PresetControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 验证预置位号是否有效
     *
     * @param presetNumber 预置位号 (1-255)
     * @return 是否有效
     */
    public static boolean isValidPresetNumber(int presetNumber) {
        return presetNumber >= 1 && presetNumber <= 255;
    }
}