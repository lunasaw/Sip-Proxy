package io.github.lunasaw.gb28181.common.entity.control.instruction.manager;

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PTZ指令映射管理器
 * 统一管理所有PTZ控制指令的映射关系和静态枚举
 */
@Slf4j
public class PTZInstructionManager {

    // 指令码到枚举的映射缓存
    private static final Map<Byte, InstructionType> INSTRUCTION_TYPE_MAP = new ConcurrentHashMap<>();
    private static final Map<Byte, Object> ALL_INSTRUCTIONS_MAP = new ConcurrentHashMap<>();

    // 名称到枚举的映射缓存
    private static final Map<String, Object> NAME_TO_ENUM_MAP = new ConcurrentHashMap<>();

    static {
        initializeMappings();
    }

    /**
     * 初始化所有指令映射
     */
    private static void initializeMappings() {
        log.info("初始化PTZ指令映射...");

        // 初始化PTZ控制指令
        for (PTZControlEnum ptz : PTZControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(ptz.getInstructionCode(), InstructionType.PTZ_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(ptz.getInstructionCode(), ptz);
            NAME_TO_ENUM_MAP.put(ptz.getName(), ptz);
        }

        // 初始化FI控制指令
        for (FIControlEnum fi : FIControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(fi.getInstructionCode(), InstructionType.FI_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(fi.getInstructionCode(), fi);
            NAME_TO_ENUM_MAP.put(fi.getName(), fi);
        }

        // 初始化预置位控制指令
        for (PresetControlEnum preset : PresetControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(preset.getInstructionCode(), InstructionType.PRESET_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(preset.getInstructionCode(), preset);
            NAME_TO_ENUM_MAP.put(preset.getName(), preset);
        }

        // 初始化巡航控制指令
        for (CruiseControlEnum cruise : CruiseControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(cruise.getInstructionCode(), InstructionType.CRUISE_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(cruise.getInstructionCode(), cruise);
            NAME_TO_ENUM_MAP.put(cruise.getName(), cruise);
        }

        // 初始化扫描控制指令
        for (ScanControlEnum scan : ScanControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(scan.getInstructionCode(), InstructionType.SCAN_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(scan.getInstructionCode(), scan);
            NAME_TO_ENUM_MAP.put(scan.getName(), scan);
        }

        // 初始化辅助开关控制指令
        for (AuxiliaryControlEnum aux : AuxiliaryControlEnum.values()) {
            INSTRUCTION_TYPE_MAP.put(aux.getInstructionCode(), InstructionType.AUXILIARY_CONTROL);
            ALL_INSTRUCTIONS_MAP.put(aux.getInstructionCode(), aux);
            NAME_TO_ENUM_MAP.put(aux.getName(), aux);
        }

        log.info("PTZ指令映射初始化完成，总计{}个指令", ALL_INSTRUCTIONS_MAP.size());
    }

    /**
     * 根据指令码获取指令类型
     *
     * @param instructionCode 指令码
     * @return 指令类型
     */
    public static InstructionType getInstructionType(byte instructionCode) {
        return INSTRUCTION_TYPE_MAP.get(instructionCode);
    }

    /**
     * 根据指令码获取具体的枚举对象
     *
     * @param instructionCode 指令码
     * @return 枚举对象
     */
    public static Object getInstructionEnum(byte instructionCode) {
        return ALL_INSTRUCTIONS_MAP.get(instructionCode);
    }

    /**
     * 根据指令码获取PTZ控制枚举
     *
     * @param instructionCode 指令码
     * @return PTZ控制枚举
     */
    public static PTZControlEnum getPTZControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof PTZControlEnum ? (PTZControlEnum) instruction : null;
    }

    /**
     * 根据指令码获取FI控制枚举
     *
     * @param instructionCode 指令码
     * @return FI控制枚举
     */
    public static FIControlEnum getFIControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof FIControlEnum ? (FIControlEnum) instruction : null;
    }

    /**
     * 根据指令码获取预置位控制枚举
     *
     * @param instructionCode 指令码
     * @return 预置位控制枚举
     */
    public static PresetControlEnum getPresetControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof PresetControlEnum ? (PresetControlEnum) instruction : null;
    }

    /**
     * 根据指令码获取巡航控制枚举
     *
     * @param instructionCode 指令码
     * @return 巡航控制枚举
     */
    public static CruiseControlEnum getCruiseControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof CruiseControlEnum ? (CruiseControlEnum) instruction : null;
    }

    /**
     * 根据指令码获取扫描控制枚举
     *
     * @param instructionCode 指令码
     * @return 扫描控制枚举
     */
    public static ScanControlEnum getScanControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof ScanControlEnum ? (ScanControlEnum) instruction : null;
    }

    /**
     * 根据指令码获取辅助开关控制枚举
     *
     * @param instructionCode 指令码
     * @return 辅助开关控制枚举
     */
    public static AuxiliaryControlEnum getAuxiliaryControlEnum(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        return instruction instanceof AuxiliaryControlEnum ? (AuxiliaryControlEnum) instruction : null;
    }

    /**
     * 根据名称获取枚举对象
     *
     * @param name 指令名称
     * @return 枚举对象
     */
    public static Object getInstructionByName(String name) {
        return NAME_TO_ENUM_MAP.get(name);
    }

    /**
     * 获取所有支持的指令码
     *
     * @return 指令码集合
     */
    public static Set<Byte> getAllSupportedInstructionCodes() {
        return new HashSet<>(ALL_INSTRUCTIONS_MAP.keySet());
    }

    /**
     * 获取指定类型的所有指令码
     *
     * @param type 指令类型
     * @return 指令码集合
     */
    public static Set<Byte> getInstructionCodesByType(InstructionType type) {
        Set<Byte> codes = new HashSet<>();
        for (Map.Entry<Byte, InstructionType> entry : INSTRUCTION_TYPE_MAP.entrySet()) {
            if (entry.getValue() == type) {
                codes.add(entry.getKey());
            }
        }
        return codes;
    }

    /**
     * 检查指令码是否被支持
     *
     * @param instructionCode 指令码
     * @return 是否支持
     */
    public static boolean isSupportedInstructionCode(byte instructionCode) {
        return ALL_INSTRUCTIONS_MAP.containsKey(instructionCode);
    }

    /**
     * 获取指令的描述信息
     *
     * @param instructionCode 指令码
     * @return 描述信息
     */
    public static String getInstructionDescription(byte instructionCode) {
        Object instruction = ALL_INSTRUCTIONS_MAP.get(instructionCode);
        if (instruction == null) {
            return "未知指令";
        }

        if (instruction instanceof PTZControlEnum) {
            return ((PTZControlEnum) instruction).getDescription();
        } else if (instruction instanceof FIControlEnum) {
            return ((FIControlEnum) instruction).getDescription();
        } else if (instruction instanceof PresetControlEnum) {
            return ((PresetControlEnum) instruction).getDescription();
        } else if (instruction instanceof CruiseControlEnum) {
            return ((CruiseControlEnum) instruction).getDescription();
        } else if (instruction instanceof ScanControlEnum) {
            return ((ScanControlEnum) instruction).getDescription();
        } else if (instruction instanceof AuxiliaryControlEnum) {
            return ((AuxiliaryControlEnum) instruction).getDescription();
        }

        return "未知指令类型";
    }

    /**
     * 获取指令统计信息
     *
     * @return 统计信息
     */
    public static InstructionStatistics getStatistics() {
        Map<InstructionType, Integer> counts = new HashMap<>();
        for (InstructionType type : InstructionType.values()) {
            counts.put(type, getInstructionCodesByType(type).size());
        }
        return new InstructionStatistics(counts, ALL_INSTRUCTIONS_MAP.size());
    }

    /**
     * 指令类型枚举
     */
    @Getter
    public enum InstructionType {
        PTZ_CONTROL("PTZ控制", "云台平移、倾斜、变倍控制"),
        FI_CONTROL("FI控制", "聚焦和光圈控制"),
        PRESET_CONTROL("预置位控制", "预置位设置、调用、删除"),
        CRUISE_CONTROL("巡航控制", "巡航路径和速度控制"),
        SCAN_CONTROL("扫描控制", "自动扫描边界和速度控制"),
        AUXILIARY_CONTROL("辅助控制", "辅助开关和设备控制");

        private final String name;
        private final String description;

        InstructionType(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    /**
     * 指令统计信息类
     */
    public static class InstructionStatistics {
        @Getter
        private final Map<InstructionType, Integer> countsByType;
        @Getter
        private final int totalCount;

        public InstructionStatistics(Map<InstructionType, Integer> countsByType, int totalCount) {
            this.countsByType = countsByType;
            this.totalCount = totalCount;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PTZ指令统计信息:\n");
            sb.append("总指令数: ").append(totalCount).append("\n");
            for (Map.Entry<InstructionType, Integer> entry : countsByType.entrySet()) {
                sb.append(entry.getKey().getName()).append(": ").append(entry.getValue()).append("个\n");
            }
            return sb.toString();
        }
    }

    /**
     * 重新加载指令映射 (用于动态更新)
     */
    public static synchronized void reloadMappings() {
        log.info("重新加载PTZ指令映射...");
        INSTRUCTION_TYPE_MAP.clear();
        ALL_INSTRUCTIONS_MAP.clear();
        NAME_TO_ENUM_MAP.clear();
        initializeMappings();
    }
}