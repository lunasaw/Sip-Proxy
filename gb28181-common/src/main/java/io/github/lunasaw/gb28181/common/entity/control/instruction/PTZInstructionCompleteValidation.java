package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import io.github.lunasaw.gb28181.common.entity.control.instruction.manager.PTZInstructionManager;
import io.github.lunasaw.gb28181.common.entity.control.instruction.serializer.PTZInstructionSerializer;

/**
 * PTZ指令系统完整验证程序
 * 手动验证所有指令的生成和解析是否正确
 */
public class PTZInstructionCompleteValidation {

    private static int testCount = 0;
    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("PTZ指令系统完整验证程序");
        System.out.println("===============================================");

        try {
            // 1. PTZ控制指令验证
            validatePTZControlInstructions();

            // 2. FI控制指令验证
            validateFIControlInstructions();

            // 3. 预置位指令验证
            validatePresetInstructions();

            // 4. 巡航指令验证
            validateCruiseInstructions();

            // 5. 扫描指令验证
            validateScanInstructions();

            // 6. 辅助开关指令验证
            validateAuxiliaryInstructions();

            // 7. 指令格式验证
            validateInstructionFormat();

            // 8. 边界值验证
            validateBoundaryValues();

            // 9. 序列化验证
            validateSerialization();

            // 10. 管理器验证
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
            System.out.println("🎉 所有测试通过！PTZ指令系统验证成功！");
        } else {
            System.out.println("❌ 有 " + failCount + " 个测试失败！");
        }
    }

    private static void validatePTZControlInstructions() {
        System.out.println("\\n1. PTZ控制指令验证...");

        // 测试停止指令
        test("PTZ停止指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x00 : "停止指令码应为0x00";
            assert instruction.isValid() : "指令应有效";
            assert instruction.toHexString().startsWith("A5") : "应以A5开头";
            return true;
        });

        // 测试右移指令
        test("PTZ右移指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0x40)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x01 : "右移指令码应为0x01";
            assert instruction.getData1() == (byte) 0x40 : "水平速度应为0x40";
            assert instruction.isValid() : "指令应有效";
            return true;
        });

        // 测试组合控制指令
        test("PTZ组合控制指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                            PTZControlEnum.TiltDirection.UP,
                            PTZControlEnum.ZoomDirection.OUT)
                    .horizontalSpeed(0x50)
                    .verticalSpeed(0x60)
                    .zoomSpeed(0x08)
                    .build();

            // 右(0x01) + 上(0x08) + 缩小(0x20) = 0x29
            assert instruction.getInstructionCode() == (byte) 0x29 : "组合指令码应为0x29";
            assert instruction.getData1() == (byte) 0x50 : "水平速度应为0x50";
            assert instruction.getData2() == (byte) 0x60 : "垂直速度应为0x60";
            assert instruction.getData3() == (byte) 0x08 : "变倍速度应为0x08";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateFIControlInstructions() {
        System.out.println("\\n2. FI控制指令验证...");

        // 测试光圈放大指令
        test("FI光圈放大指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x002)
                    .addFIControl(FIControlEnum.IRIS_OPEN)
                    .irisSpeed(0x60)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x44 : "光圈放大指令码应为0x44";
            assert instruction.getData2() == (byte) 0x60 : "光圈速度应为0x60";
            assert instruction.isValid() : "指令应有效";

            // 验证FI指令的位模式
            byte code = instruction.getInstructionCode();
            assert (code & 0xF0) == 0x40 : "FI指令高4位应为0100";
            return true;
        });

        // 测试聚焦近指令
        test("FI聚焦近指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x003)
                    .addFIControl(FIControlEnum.FOCUS_NEAR)
                    .focusSpeed(0x90)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x42 : "聚焦近指令码应为0x42";
            assert instruction.getData1() == (byte) 0x90 : "聚焦速度应为0x90";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validatePresetInstructions() {
        System.out.println("\\n3. 预置位指令验证...");

        // 测试设置预置位指令
        test("设置预置位指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x004)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 10)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x81 : "设置预置位指令码应为0x81";
            assert instruction.getData1() == (byte) 0x00 : "字节5应为0x00";
            assert instruction.getData2() == (byte) 0x0A : "预置位号应为10";
            assert instruction.isValid() : "指令应有效";
            return true;
        });

        // 测试调用预置位指令
        test("调用预置位指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x005)
                    .addPresetControl(PresetControlEnum.CALL_PRESET, 255)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x82 : "调用预置位指令码应为0x82";
            assert instruction.getData2() == (byte) 0xFF : "预置位号应为255";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateCruiseInstructions() {
        System.out.println("\\n4. 巡航指令验证...");

        // 测试设置巡航速度指令
        test("设置巡航速度指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x006)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 4095)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x86 : "设置巡航速度指令码应为0x86";
            assert instruction.getData1() == (byte) 0x01 : "巡航组号应为1";
            assert instruction.getData2() == (byte) 0xFF : "速度低8位应为0xFF";
            assert instruction.getData3() == (byte) 0x0F : "速度高4位应为0x0F";
            assert instruction.isValid() : "指令应有效";

            // 验证数据解码
            int decodedSpeed = (instruction.getData2() & 0xFF) |
                    ((instruction.getData3() & 0x0F) << 8);
            assert decodedSpeed == 4095 : "解码速度应为4095";
            return true;
        });

        // 测试开始巡航指令
        test("开始巡航指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x007)
                    .addCruiseControl(CruiseControlEnum.START_CRUISE, 5)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x88 : "开始巡航指令码应为0x88";
            assert instruction.getData1() == (byte) 0x05 : "巡航组号应为5";
            assert instruction.getData2() == (byte) 0x00 : "字节6应为0x00";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateScanInstructions() {
        System.out.println("\\n5. 扫描指令验证...");

        // 测试设置扫描速度指令
        test("设置扫描速度指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x008)
                    .addScanSpeedControl(2, 2048)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x8A : "设置扫描速度指令码应为0x8A";
            assert instruction.getData1() == (byte) 0x02 : "扫描组号应为2";
            assert instruction.getData2() == (byte) 0x00 : "速度低8位应为0x00";
            assert instruction.getData3() == (byte) 0x08 : "速度高4位应为0x08";
            assert instruction.isValid() : "指令应有效";
            return true;
        });

        // 测试开始扫描指令
        test("开始扫描指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x009)
                    .addScanControl(ScanControlEnum.START_AUTO_SCAN, 3,
                            ScanControlEnum.ScanOperationType.START)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x89 : "开始扫描指令码应为0x89";
            assert instruction.getData1() == (byte) 0x03 : "扫描组号应为3";
            assert instruction.getData2() == (byte) 0x00 : "操作类型应为0x00";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateAuxiliaryInstructions() {
        System.out.println("\\n6. 辅助开关指令验证...");

        // 测试开关开启指令
        test("开关开启指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x00A)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x8C : "开关开启指令码应为0x8C";
            assert instruction.getData1() == (byte) 0x01 : "开关编号应为1";
            assert instruction.isValid() : "指令应有效";
            return true;
        });

        // 测试开关关闭指令
        test("开关关闭指令", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x00B)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 255)
                    .build();

            assert instruction.getInstructionCode() == (byte) 0x8D : "开关关闭指令码应为0x8D";
            assert instruction.getData1() == (byte) 0xFF : "开关编号应为255";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateInstructionFormat() {
        System.out.println("\\n7. 指令格式验证...");

        // 测试指令格式基本要求
        test("指令格式基本要求", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x123)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0x40)
                    .verticalSpeed(0x80)
                    .zoomSpeed(0x0F)
                    .build();

            // 验证首字节
            assert instruction.getHeader() == (byte) 0xA5 : "首字节应为0xA5";

            // 验证长度
            assert instruction.toByteArray().length == 8 : "字节数组长度应为8";
            assert instruction.toHexString().length() == 16 : "十六进制字符串长度应为16";

            // 验证地址编码
            assert instruction.getFullAddress() == 0x123 : "完整地址应为0x123";
            assert instruction.getAddressLow() == (byte) 0x23 : "地址低8位应为0x23";
            assert (instruction.getCombinationCode2() & 0x0F) == 0x01 : "地址高4位应为0x01";

            // 验证数据编码
            assert instruction.getData1() == (byte) 0x40 : "数据1应为0x40";
            assert instruction.getData2() == (byte) 0x80 : "数据2应为0x80";
            assert instruction.getData3() == (byte) 0x0F : "数据3应为0x0F";

            assert instruction.isValid() : "指令应有效";
            return true;
        });

        // 测试校验码计算
        test("校验码计算", () -> {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();

            // 手动计算校验码
            byte[] bytes = instruction.toByteArray();
            int sum = 0;
            for (int i = 0; i < 7; i++) {
                sum += (bytes[i] & 0xFF);
            }
            byte expectedChecksum = (byte) (sum % 256);

            assert instruction.getChecksum() == expectedChecksum : "校验码计算错误";
            assert instruction.isValid() : "指令应有效";
            return true;
        });
    }

    private static void validateBoundaryValues() {
        System.out.println("\\n8. 边界值验证...");

        // 测试地址边界值
        test("地址边界值", () -> {
            // 最小地址
            PTZInstructionFormat minAddr = PTZInstructionBuilder.create()
                    .address(0x000)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
            assert minAddr.getFullAddress() == 0x000 : "最小地址应为0x000";
            assert minAddr.isValid() : "最小地址指令应有效";

            // 最大地址
            PTZInstructionFormat maxAddr = PTZInstructionBuilder.create()
                    .address(0xFFF)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
            assert maxAddr.getFullAddress() == 0xFFF : "最大地址应为0xFFF";
            assert maxAddr.isValid() : "最大地址指令应有效";
            return true;
        });

        // 测试速度边界值
        test("速度边界值", () -> {
            // 最大速度值
            PTZInstructionFormat maxSpeed = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0xFF)
                    .verticalSpeed(0xFF)
                    .zoomSpeed(0x0F)
                    .build();
            assert maxSpeed.getData1() == (byte) 0xFF : "最大水平速度应为0xFF";
            assert maxSpeed.getData2() == (byte) 0xFF : "最大垂直速度应为0xFF";
            assert maxSpeed.getData3() == (byte) 0x0F : "最大变倍速度应为0x0F";
            assert maxSpeed.isValid() : "最大速度指令应有效";
            return true;
        });
    }

    private static void validateSerialization() {
        System.out.println("\\n9. 序列化验证...");

        // 测试序列化一致性
        test("序列化一致性", () -> {
            PTZInstructionFormat original = PTZInstructionBuilder.create()
                    .address(0x200)
                    .addFIControl(FIControlEnum.IRIS_OPEN)
                    .irisSpeed(0x90)
                    .build();

            // 十六进制序列化
            String hex = PTZInstructionSerializer.serializeToHex(original);
            PTZInstructionFormat fromHex = PTZInstructionSerializer.deserializeFromHex(hex);
            assert original.getInstructionCode() == fromHex.getInstructionCode() : "十六进制序列化不一致";
            assert fromHex.isValid() : "从十六进制反序列化的指令应有效";

            // Base64序列化
            String base64 = PTZInstructionSerializer.serializeToBase64(original);
            PTZInstructionFormat fromBase64 = PTZInstructionSerializer.deserializeFromBase64(base64);
            assert original.getInstructionCode() == fromBase64.getInstructionCode() : "Base64序列化不一致";
            assert fromBase64.isValid() : "从Base64反序列化的指令应有效";

            return true;
        });
    }

    private static void validateInstructionManager() {
        System.out.println("\\n10. 指令管理器验证...");

        // 测试指令管理器功能
        test("指令管理器功能", () -> {
            // 测试指令类型识别
            assert PTZInstructionManager.getInstructionType((byte) 0x01) ==
                    PTZInstructionManager.InstructionType.PTZ_CONTROL : "PTZ指令类型识别错误";
            assert PTZInstructionManager.getInstructionType((byte) 0x40) ==
                    PTZInstructionManager.InstructionType.FI_CONTROL : "FI指令类型识别错误";
            assert PTZInstructionManager.getInstructionType((byte) 0x81) ==
                    PTZInstructionManager.InstructionType.PRESET_CONTROL : "预置位指令类型识别错误";

            // 测试枚举获取
            assert PTZInstructionManager.getPTZControlEnum((byte) 0x01) == PTZControlEnum.PAN_RIGHT :
                    "PTZ枚举获取错误";
            assert PTZInstructionManager.getFIControlEnum((byte) 0x40) == FIControlEnum.STOP :
                    "FI枚举获取错误";

            // 测试统计信息
            PTZInstructionManager.InstructionStatistics stats = PTZInstructionManager.getStatistics();
            assert stats.getTotalCount() > 0 : "统计总数应大于0";

            return true;
        });
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