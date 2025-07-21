package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import io.github.lunasaw.gb28181.common.entity.control.instruction.manager.PTZInstructionManager;

/**
 * PTZ指令系统核心验证程序（无外部依赖版本）
 * 验证所有核心指令生成和解析功能
 */
public class PTZInstructionCoreValidation {

    private static int testCount = 0;
    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("PTZ指令系统核心验证程序");
        System.out.println("===============================================");

        try {
            // 核心功能验证
            validateCoreInstructions();
            validateInstructionFormat();
            validateBoundaryValues();
            validateBasicSerialization();
            validateInstructionManager();

        } catch (Exception e) {
            System.err.println("验证过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }

        // 输出验证结果
        System.out.println("===============================================");
        System.out.println("验证结果汇总:");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过数: " + passCount);
        System.out.println("失败数: " + failCount);
        System.out.println("成功率: " + String.format("%.2f%%", (double) passCount / testCount * 100));
        System.out.println("===============================================");

        if (failCount == 0) {
            System.out.println("🎉 所有核心测试通过！PTZ指令系统验证成功！");
        } else {
            System.out.println("❌ 有 " + failCount + " 个测试失败！");
        }
    }

    private static void validateCoreInstructions() {
        System.out.println("\\n1. 核心指令验证...");

        // PTZ指令验证
        test("PTZ停止指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x00 && instruction.isValid();
        });

        test("PTZ右移指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0x40)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x01 &&
                    instruction.getData1() == (byte) 0x40 && instruction.isValid();
        });

        test("PTZ组合控制指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                            PTZControlEnum.TiltDirection.UP,
                            PTZControlEnum.ZoomDirection.OUT)
                    .horizontalSpeed(0x50)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x29 && instruction.isValid();
        });

        // FI指令验证
        test("FI光圈放大指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x002)
                    .addFIControl(FIControlEnum.IRIS_OPEN)
                    .irisSpeed(0x60)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x44 &&
                    instruction.getData2() == (byte) 0x60 && instruction.isValid();
        });

        // 预置位指令验证
        test("设置预置位指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x004)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 10)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x81 &&
                    instruction.getData2() == (byte) 0x0A && instruction.isValid();
        });

        // 巡航指令验证
        test("设置巡航速度指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x006)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 4095)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x86 &&
                    instruction.getData2() == (byte) 0xFF &&
                    instruction.getData3() == (byte) 0x0F && instruction.isValid();
        });

        // 扫描指令验证
        test("设置扫描速度指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x008)
                    .addScanSpeedControl(2, 2048)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x8A &&
                    instruction.getData1() == (byte) 0x02 &&
                    instruction.getData2() == (byte) 0x00 &&
                    instruction.getData3() == (byte) 0x08 && instruction.isValid();
        });

        // 辅助开关指令验证
        test("开关开启指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x00A)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                    .build();
            return instruction.getInstructionCode() == (byte) 0x8C &&
                    instruction.getData1() == (byte) 0x01 && instruction.isValid();
        });
    }

    private static void validateInstructionFormat() {
        System.out.println("\\n2. 指令格式验证...");

        test("指令格式基本要求", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x123)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0x40)
                    .verticalSpeed(0x80)
                    .zoomSpeed(0x0F)
                    .build();

            return instruction.getHeader() == (byte) 0xA5 &&
                    instruction.toByteArray().length == 8 &&
                    instruction.toHexString().length() == 16 &&
                    instruction.getFullAddress() == 0x123 &&
                    instruction.getData1() == (byte) 0x40 &&
                    instruction.getData2() == (byte) 0x80 &&
                    instruction.getData3() == (byte) 0x0F &&
                    instruction.isValid();
        });

        test("校验码计算", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();

            byte[] bytes = instruction.toByteArray();
            int sum = 0;
            for (int i = 0; i < 7; i++) {
                sum += (bytes[i] & 0xFF);
            }
            byte expectedChecksum = (byte) (sum % 256);

            return instruction.getChecksum() == expectedChecksum && instruction.isValid();
        });
    }

    private static void validateBoundaryValues() {
        System.out.println("\\n3. 边界值验证...");

        test("地址边界值", () -> {
            // 最小地址
            PTZInstructionFormat minAddr = PTZInstructionBuilder.create()
                    .address(0x000)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();

            // 最大地址
            PTZInstructionFormat maxAddr = PTZInstructionBuilder.create()
                    .address(0xFFF)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();

            return minAddr.getFullAddress() == 0x000 && minAddr.isValid() &&
                    maxAddr.getFullAddress() == 0xFFF && maxAddr.isValid();
        });

        test("速度边界值", () -> {
            PTZInstructionFormat maxSpeed = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0xFF)
                    .verticalSpeed(0xFF)
                    .zoomSpeed(0x0F)
                    .build();

            return maxSpeed.getData1() == (byte) 0xFF &&
                    maxSpeed.getData2() == (byte) 0xFF &&
                    maxSpeed.getData3() == (byte) 0x0F &&
                    maxSpeed.isValid();
        });

        test("预置位边界值", () -> {
            PTZInstructionFormat minPreset = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                    .build();

            PTZInstructionFormat maxPreset = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 255)
                    .build();

            return minPreset.getData2() == (byte) 1 && minPreset.isValid() &&
                    maxPreset.getData2() == (byte) 0xFF && maxPreset.isValid();
        });
    }

    private static void validateBasicSerialization() {
        System.out.println("\\n4. 基础序列化验证...");

        test("字节数组序列化", () -> {
            PTZInstructionFormat original = PTZInstructionBuilder.create()
                    .address(0x200)
                    .addFIControl(FIControlEnum.IRIS_OPEN)
                    .irisSpeed(0x90)
                    .build();

            byte[] bytes = original.toByteArray();
            PTZInstructionFormat reconstructed = PTZInstructionFormat.fromByteArray(bytes);

            return bytes.length == 8 &&
                    original.getInstructionCode() == reconstructed.getInstructionCode() &&
                    original.getData2() == reconstructed.getData2() &&
                    reconstructed.isValid();
        });

        test("十六进制序列化", () -> {
            PTZInstructionFormat original = PTZInstructionBuilder.create()
                    .address(0x123)
                    .addPTZControl(PTZControlEnum.PAN_LEFT)
                    .horizontalSpeed(0x60)
                    .build();

            String hex = original.toHexString();
            PTZInstructionFormat reconstructed = PTZInstructionFormat.fromHexString(hex);

            return hex.length() == 16 &&
                    hex.startsWith("A5") &&
                    original.getInstructionCode() == reconstructed.getInstructionCode() &&
                    reconstructed.isValid();
        });
    }

    private static void validateInstructionManager() {
        System.out.println("\\n5. 指令管理器验证...");

        test("指令类型识别", () -> {
            return PTZInstructionManager.getInstructionType((byte) 0x01) ==
                    PTZInstructionManager.InstructionType.PTZ_CONTROL &&
                    PTZInstructionManager.getInstructionType((byte) 0x40) ==
                            PTZInstructionManager.InstructionType.FI_CONTROL &&
                    PTZInstructionManager.getInstructionType((byte) 0x81) ==
                            PTZInstructionManager.InstructionType.PRESET_CONTROL;
        });

        test("枚举获取", () -> {
            return PTZInstructionManager.getPTZControlEnum((byte) 0x01) == PTZControlEnum.PAN_RIGHT &&
                    PTZInstructionManager.getFIControlEnum((byte) 0x40) == FIControlEnum.STOP &&
                    PTZInstructionManager.getPresetControlEnum((byte) 0x81) == PresetControlEnum.SET_PRESET;
        });

        test("统计信息", () -> {
            PTZInstructionManager.InstructionStatistics stats = PTZInstructionManager.getStatistics();
            return stats.getTotalCount() > 0 && stats.getCountsByType().size() > 0;
        });
    }

    // 测试范例生成验证
    private static void demonstrateInstructionExamples() {
        System.out.println("\\n6. 指令生成示例演示...");

        // 演示表A.5中的PTZ指令示例
        System.out.println("\\n表A.5 PTZ指令示例验证:");

        // 序号7: 停止指令
        PTZInstructionFormat stopCmd = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .build();
        System.out.println("停止指令: " + stopCmd.toHexString());

        // 序号6: 向右指令
        PTZInstructionFormat rightCmd = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .build();
        System.out.println("右移指令: " + rightCmd.toHexString());

        // 序号8: 组合指令示例
        PTZInstructionFormat combinedCmd = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                        PTZControlEnum.TiltDirection.UP,
                        PTZControlEnum.ZoomDirection.OUT)
                .horizontalSpeed(0x50)
                .verticalSpeed(0x60)
                .zoomSpeed(0x08)
                .build();
        System.out.println("组合指令: " + combinedCmd.toHexString());

        // 演示表A.7中的FI指令示例
        System.out.println("\\n表A.7 FI指令示例验证:");

        // 序号1: 光圈缩小
        PTZInstructionFormat irisClose = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.IRIS_CLOSE)
                .irisSpeed(0x80)
                .build();
        System.out.println("光圈缩小: " + irisClose.toHexString());

        // 序号3: 聚焦近
        PTZInstructionFormat focusNear = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.FOCUS_NEAR)
                .focusSpeed(0x70)
                .build();
        System.out.println("聚焦近: " + focusNear.toHexString());

        // 演示表A.8中的预置位指令
        System.out.println("\\n表A.8 预置位指令示例验证:");

        // 设置预置位
        PTZInstructionFormat setPreset = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 5)
                .build();
        System.out.println("设置预置位5: " + setPreset.toHexString());

        // 调用预置位
        PTZInstructionFormat callPreset = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 10)
                .build();
        System.out.println("调用预置位10: " + callPreset.toHexString());
    }

    private static void test(String testName, TestFunction testFunction) {
        testCount++;
        try {
            boolean result = testFunction.run();
            if (result) {
                System.out.println("  ✓ " + testName + " - 通过");
                passCount++;
            } else {
                System.out.println("  ✗ " + testName + " - 失败");
                failCount++;
            }
        } catch (Exception e) {
            System.out.println("  ✗ " + testName + " - 异常: " + e.getMessage());
            failCount++;
        }
    }

    @FunctionalInterface
    private interface TestFunction {
        boolean run() throws Exception;
    }
}