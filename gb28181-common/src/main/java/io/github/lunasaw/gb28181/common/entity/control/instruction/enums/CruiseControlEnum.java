package io.github.lunasaw.gb28181.common.entity.control.instruction.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 巡航指令枚举
 * 根据 A.3.5 巡航指令 规范实现
 */
@Getter
@AllArgsConstructor
public enum CruiseControlEnum {

    ADD_CRUISE_POINT("加入巡航点", (byte) 0x84, "加入巡航点"),
    DELETE_CRUISE_POINT("删除巡航点", (byte) 0x85, "删除一个巡航点"),
    SET_CRUISE_SPEED("设置巡航速度", (byte) 0x86, "设置巡航速度"),
    SET_CRUISE_STAY_TIME("设置巡航停留时间", (byte) 0x87, "设置巡航停留时间"),
    START_CRUISE("开始巡航", (byte) 0x88, "开始巡航");

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
    private static final Map<Byte, CruiseControlEnum> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(CruiseControlEnum::getInstructionCode, Function.identity()));

    private static final Map<String, CruiseControlEnum> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(CruiseControlEnum::getName, Function.identity()));

    /**
     * 根据指令码查找枚举
     */
    public static CruiseControlEnum getByCode(byte code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据名称查找枚举
     */
    public static CruiseControlEnum getByName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 验证巡航组号是否有效
     *
     * @param groupNumber 巡航组号 (0-255)
     * @return 是否有效
     */
    public static boolean isValidGroupNumber(int groupNumber) {
        return groupNumber >= 0 && groupNumber <= 255;
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

    /**
     * 验证速度值是否有效
     *
     * @param speed 速度 (数据的低8位在字节6，高4位在字节7的高4位)
     * @return 是否有效
     */
    public static boolean isValidSpeed(int speed) {
        return speed >= 0 && speed <= 0xFFF; // 12位数据
    }

    /**
     * 验证停留时间是否有效
     *
     * @param stayTime 停留时间(秒) (数据的低8位在字节6，高4位在字节7的高4位)
     * @return 是否有效
     */
    public static boolean isValidStayTime(int stayTime) {
        return stayTime >= 0 && stayTime <= 0xFFF; // 12位数据
    }
}