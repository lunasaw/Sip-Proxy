package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FI(Focus/Iris)控制指令生成和解析完整测试
 * 验证所有FI指令是否按照A.3.3规范正确生成
 */
class FIControlInstructionTest {

    @Test
    @DisplayName("FI停止指令测试")
    void testFIStopInstruction() {
        // 根据表A.7序号5：字节4=40H，镜头停止FI的所有动作
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.STOP)
                .focusSpeed(0x00)
                .irisSpeed(0x00)
                .build();

        assertEquals((byte) 0x40, instruction.getInstructionCode());
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 验证FI指令的固定位模式 (Bit7=0, Bit6=1, Bit5=0, Bit4=0)
        byte instructionCode = instruction.getInstructionCode();
        assertEquals(0x40, instructionCode & 0xF0); // 高4位应该是0100
    }

    @Test
    @DisplayName("光圈控制指令测试")
    void testIrisControl() {
        // 测试光圈缩小 - 表A.7序号1
        PTZInstructionFormat irisCloseInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addFIControl(FIControlEnum.IRIS_CLOSE)
                .focusSpeed(0x00)
                .irisSpeed(0x80)
                .build();

        assertEquals((byte) 0x48, irisCloseInstruction.getInstructionCode());
        assertEquals((byte) 0x00, irisCloseInstruction.getData1()); // 聚焦速度
        assertEquals((byte) 0x80, irisCloseInstruction.getData2()); // 光圈速度
        assertTrue(irisCloseInstruction.isValid());

        // 验证光圈控制位 (Bit3=1)
        assertTrue((irisCloseInstruction.getInstructionCode() & 0x08) != 0);

        // 测试光圈放大 - 表A.7序号2
        PTZInstructionFormat irisOpenInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .focusSpeed(0x00)
                .irisSpeed(0x60)
                .build();

        assertEquals((byte) 0x44, irisOpenInstruction.getInstructionCode());
        assertEquals((byte) 0x00, irisOpenInstruction.getData1());
        assertEquals((byte) 0x60, irisOpenInstruction.getData2());
        assertTrue(irisOpenInstruction.isValid());

        // 验证光圈控制位 (Bit2=1)
        assertTrue((irisOpenInstruction.getInstructionCode() & 0x04) != 0);
    }

    @Test
    @DisplayName("聚焦控制指令测试")
    void testFocusControl() {
        // 测试聚焦近 - 表A.7序号3
        PTZInstructionFormat focusNearInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addFIControl(FIControlEnum.FOCUS_NEAR)
                .focusSpeed(0x90)
                .irisSpeed(0x00)
                .build();

        assertEquals((byte) 0x42, focusNearInstruction.getInstructionCode());
        assertEquals((byte) 0x90, focusNearInstruction.getData1()); // 聚焦速度
        assertEquals((byte) 0x00, focusNearInstruction.getData2()); // 光圈速度
        assertTrue(focusNearInstruction.isValid());

        // 验证聚焦控制位 (Bit1=1)
        assertTrue((focusNearInstruction.getInstructionCode() & 0x02) != 0);

        // 测试聚焦远 - 表A.7序号4
        PTZInstructionFormat focusFarInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addFIControl(FIControlEnum.FOCUS_FAR)
                .focusSpeed(0x70)
                .irisSpeed(0x00)
                .build();

        assertEquals((byte) 0x41, focusFarInstruction.getInstructionCode());
        assertEquals((byte) 0x70, focusFarInstruction.getData1());
        assertEquals((byte) 0x00, focusFarInstruction.getData2());
        assertTrue(focusFarInstruction.isValid());

        // 验证聚焦控制位 (Bit0=1)
        assertTrue((focusFarInstruction.getInstructionCode() & 0x01) != 0);
    }

    @Test
    @DisplayName("FI组合控制指令测试")
    void testFICombinedControl() {
        // 测试光圈缩小+聚焦远组合 - 表A.7序号6
        PTZInstructionFormat combinedInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addFIControl(FIControlEnum.IrisDirection.CLOSE,
                        FIControlEnum.FocusDirection.FAR)
                .focusSpeed(0x80)
                .irisSpeed(0x60)
                .build();

        // 验证指令码：基础(0x40) + 光圈缩小(0x08) + 聚焦远(0x01) = 0x49
        assertEquals((byte) 0x49, combinedInstruction.getInstructionCode());
        assertEquals((byte) 0x80, combinedInstruction.getData1()); // 聚焦速度
        assertEquals((byte) 0x60, combinedInstruction.getData2()); // 光圈速度
        assertTrue(combinedInstruction.isValid());

        // 验证控制类型检查
        FIControlEnum controlEnum = FIControlEnum.getByCode(combinedInstruction.getInstructionCode());
        if (controlEnum != null) {
            assertTrue(controlEnum.hasIrisControl());
            assertTrue(controlEnum.hasFocusControl());
        }

        // 测试其他组合：光圈放大+聚焦近
        PTZInstructionFormat anotherCombination = PTZInstructionBuilder.create()
                .address(0x005)
                .addFIControl(FIControlEnum.IrisDirection.OPEN,
                        FIControlEnum.FocusDirection.NEAR)
                .focusSpeed(0x50)
                .irisSpeed(0x70)
                .build();

        // 基础(0x40) + 光圈放大(0x04) + 聚焦近(0x02) = 0x46
        assertEquals((byte) 0x46, anotherCombination.getInstructionCode());
        assertTrue(anotherCombination.isValid());
    }

    @ParameterizedTest
    @DisplayName("FI指令码映射验证")
    @CsvSource({
            "0x40, STOP, 停止",
            "0x41, FOCUS_FAR, 聚焦远",
            "0x42, FOCUS_NEAR, 聚焦近",
            "0x44, IRIS_OPEN, 光圈放大",
            "0x48, IRIS_CLOSE, 光圈缩小"
    })
    void testFIInstructionCodeMapping(String hexCode, String enumName, String description) {
        byte code = (byte) Integer.parseInt(hexCode.substring(2), 16);
        FIControlEnum controlEnum = FIControlEnum.getByCode(code);

        assertNotNull(controlEnum, "指令码 " + hexCode + " 应该有对应的枚举");
        assertEquals(enumName, controlEnum.name());
        assertEquals(description, controlEnum.getName());
    }

    @Test
    @DisplayName("FI指令解析测试")
    void testFIInstructionParsing() {
        // 创建一个光圈放大指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .focusSpeed(0x70)
                .irisSpeed(0x90)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();
        assertEquals(8, bytes.length);

        // 从字节数组重建指令
        PTZInstructionFormat parsedInstruction = PTZInstructionFormat.fromByteArray(bytes);

        // 验证解析结果
        assertEquals(originalInstruction.getFullAddress(), parsedInstruction.getFullAddress());
        assertEquals(originalInstruction.getInstructionCode(), parsedInstruction.getInstructionCode());
        assertEquals(originalInstruction.getData1(), parsedInstruction.getData1());
        assertEquals(originalInstruction.getData2(), parsedInstruction.getData2());
        assertTrue(parsedInstruction.isValid());

        // 验证十六进制字符串解析
        String hexString = originalInstruction.toHexString();
        PTZInstructionFormat parsedFromHex = PTZInstructionFormat.fromHexString(hexString);
        assertEquals(originalInstruction.getInstructionCode(), parsedFromHex.getInstructionCode());
        assertTrue(parsedFromHex.isValid());
    }

    @Test
    @DisplayName("FI指令速度范围测试")
    void testFISpeedRanges() {
        // 测试最小速度值
        PTZInstructionFormat minSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .focusSpeed(0x00)
                .irisSpeed(0x00)
                .build();

        assertTrue(minSpeedInstruction.isValid());
        assertEquals((byte) 0x00, minSpeedInstruction.getData1());
        assertEquals((byte) 0x00, minSpeedInstruction.getData2());

        // 测试最大速度值
        PTZInstructionFormat maxSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.FOCUS_NEAR)
                .focusSpeed(0xFF)
                .irisSpeed(0xFF)
                .build();

        assertTrue(maxSpeedInstruction.isValid());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData1());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData2());
    }

    @Test
    @DisplayName("FI指令位模式验证")
    void testFIBitPatterns() {
        // 验证FI指令的固定位模式
        for (FIControlEnum fiControl : FIControlEnum.values()) {
            byte code = fiControl.getInstructionCode();

            // Bit7应该为0, Bit6应该为1, Bit5和Bit4应该为0
            assertEquals(0, (code >> 7) & 1, "Bit7应该为0");
            assertEquals(1, (code >> 6) & 1, "Bit6应该为1");
            assertEquals(0, (code >> 5) & 1, "Bit5应该为0");
            assertEquals(0, (code >> 4) & 1, "Bit4应该为0");
        }
    }

    @Test
    @DisplayName("FI方向枚举测试")
    void testFIDirectionEnums() {
        // 测试光圈方向枚举
        assertEquals(FIControlEnum.IrisDirection.CLOSE,
                FIControlEnum.IRIS_CLOSE.getIrisDirection());
        assertEquals(FIControlEnum.IrisDirection.OPEN,
                FIControlEnum.IRIS_OPEN.getIrisDirection());
        assertEquals(FIControlEnum.IrisDirection.NONE,
                FIControlEnum.STOP.getIrisDirection());

        // 测试聚焦方向枚举
        assertEquals(FIControlEnum.FocusDirection.NEAR,
                FIControlEnum.FOCUS_NEAR.getFocusDirection());
        assertEquals(FIControlEnum.FocusDirection.FAR,
                FIControlEnum.FOCUS_FAR.getFocusDirection());
        assertEquals(FIControlEnum.FocusDirection.NONE,
                FIControlEnum.STOP.getFocusDirection());
    }

    @Test
    @DisplayName("FI指令互斥性验证")
    void testFIMutualExclusion() {
        // 验证Bit3和Bit2不应同时为1（光圈控制互斥）
        // 验证Bit1和Bit0不应同时为1（聚焦控制互斥）

        for (FIControlEnum fiControl : FIControlEnum.values()) {
            byte code = fiControl.getInstructionCode();

            // 检查光圈控制位互斥性
            boolean bit3 = (code & 0x08) != 0;
            boolean bit2 = (code & 0x04) != 0;
            assertFalse(bit3 && bit2, "光圈控制的Bit3和Bit2不应同时为1");

            // 检查聚焦控制位互斥性
            boolean bit1 = (code & 0x02) != 0;
            boolean bit0 = (code & 0x01) != 0;
            assertFalse(bit1 && bit0, "聚焦控制的Bit1和Bit0不应同时为1");
        }
    }
}