package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PresetControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预置位控制指令生成和解析完整测试
 * 验证所有预置位指令是否按照A.3.4规范正确生成
 */
class PresetControlInstructionTest {

    @Test
    @DisplayName("设置预置位指令测试")
    void testSetPresetInstruction() {
        // 测试设置预置位1
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                .build();

        assertEquals((byte) 0x81, instruction.getInstructionCode());
        assertEquals((byte) 0x00, instruction.getData1()); // 字节5固定为00H
        assertEquals((byte) 0x01, instruction.getData2()); // 字节6为预置位号
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试设置预置位255（最大值）
        PTZInstructionFormat maxPresetInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPresetControl(PresetControlEnum.SET_PRESET, 255)
                .build();

        assertEquals((byte) 0x81, maxPresetInstruction.getInstructionCode());
        assertEquals((byte) 0x00, maxPresetInstruction.getData1());
        assertEquals((byte) 0xFF, maxPresetInstruction.getData2()); // 255 = 0xFF
        assertTrue(maxPresetInstruction.isValid());
    }

    @Test
    @DisplayName("调用预置位指令测试")
    void testCallPresetInstruction() {
        // 测试调用预置位5
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 5)
                .build();

        assertEquals((byte) 0x82, instruction.getInstructionCode());
        assertEquals((byte) 0x00, instruction.getData1()); // 字节5固定为00H
        assertEquals((byte) 0x05, instruction.getData2()); // 字节6为预置位号
        assertEquals(0x003, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试调用预置位128（中间值）
        PTZInstructionFormat midPresetInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 128)
                .build();

        assertEquals((byte) 0x82, midPresetInstruction.getInstructionCode());
        assertEquals((byte) 0x00, midPresetInstruction.getData1());
        assertEquals((byte) 0x80, midPresetInstruction.getData2()); // 128 = 0x80
        assertTrue(midPresetInstruction.isValid());
    }

    @Test
    @DisplayName("删除预置位指令测试")
    void testDeletePresetInstruction() {
        // 测试删除预置位10
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, 10)
                .build();

        assertEquals((byte) 0x83, instruction.getInstructionCode());
        assertEquals((byte) 0x00, instruction.getData1()); // 字节5固定为00H
        assertEquals((byte) 0x0A, instruction.getData2()); // 字节6为预置位号
        assertEquals(0x005, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试删除预置位200
        PTZInstructionFormat highPresetInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, 200)
                .build();

        assertEquals((byte) 0x83, highPresetInstruction.getInstructionCode());
        assertEquals((byte) 0x00, highPresetInstruction.getData1());
        assertEquals((byte) 0xC8, highPresetInstruction.getData2()); // 200 = 0xC8
        assertTrue(highPresetInstruction.isValid());
    }

    @ParameterizedTest
    @DisplayName("预置位号范围测试")
    @ValueSource(ints = {1, 50, 100, 150, 200, 255})
    void testPresetNumberRange(int presetNumber) {
        // 测试有效预置位号
        assertTrue(PresetControlEnum.isValidPresetNumber(presetNumber));

        // 测试设置预置位
        PTZInstructionFormat setInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, presetNumber)
                .build();

        assertEquals((byte) 0x81, setInstruction.getInstructionCode());
        assertEquals((byte) presetNumber, setInstruction.getData2());
        assertTrue(setInstruction.isValid());

        // 测试调用预置位
        PTZInstructionFormat callInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPresetControl(PresetControlEnum.CALL_PRESET, presetNumber)
                .build();

        assertEquals((byte) 0x82, callInstruction.getInstructionCode());
        assertEquals((byte) presetNumber, callInstruction.getData2());
        assertTrue(callInstruction.isValid());

        // 测试删除预置位
        PTZInstructionFormat deleteInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, presetNumber)
                .build();

        assertEquals((byte) 0x83, deleteInstruction.getInstructionCode());
        assertEquals((byte) presetNumber, deleteInstruction.getData2());
        assertTrue(deleteInstruction.isValid());
    }

    @Test
    @DisplayName("预置位指令解析测试")
    void testPresetInstructionParsing() {
        // 创建设置预置位指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addPresetControl(PresetControlEnum.SET_PRESET, 42)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();
        assertEquals(8, bytes.length);

        // 验证字节数组内容
        assertEquals((byte) 0xA5, bytes[0]); // 首字节
        assertEquals((byte) 0x81, bytes[3]); // 指令码
        assertEquals((byte) 0x00, bytes[4]); // 数据1固定00H
        assertEquals((byte) 42, bytes[5]);   // 预置位号

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
        assertEquals((byte) 42, parsedFromHex.getData2());
        assertTrue(parsedFromHex.isValid());
    }

    @Test
    @DisplayName("预置位指令枚举映射测试")
    void testPresetEnumMapping() {
        // 测试指令码到枚举的映射
        assertEquals(PresetControlEnum.SET_PRESET,
                PresetControlEnum.getByCode((byte) 0x81));
        assertEquals(PresetControlEnum.CALL_PRESET,
                PresetControlEnum.getByCode((byte) 0x82));
        assertEquals(PresetControlEnum.DELETE_PRESET,
                PresetControlEnum.getByCode((byte) 0x83));

        // 测试名称到枚举的映射
        assertEquals(PresetControlEnum.SET_PRESET,
                PresetControlEnum.getByName("设置预置位"));
        assertEquals(PresetControlEnum.CALL_PRESET,
                PresetControlEnum.getByName("调用预置位"));
        assertEquals(PresetControlEnum.DELETE_PRESET,
                PresetControlEnum.getByName("删除预置位"));

        // 测试无效映射
        assertNull(PresetControlEnum.getByCode((byte) 0x80));
        assertNull(PresetControlEnum.getByCode((byte) 0x84));
        assertNull(PresetControlEnum.getByName("无效指令"));
    }

    @Test
    @DisplayName("预置位号验证测试")
    void testPresetNumberValidation() {
        // 测试有效范围 1-255
        assertTrue(PresetControlEnum.isValidPresetNumber(1));
        assertTrue(PresetControlEnum.isValidPresetNumber(255));
        assertTrue(PresetControlEnum.isValidPresetNumber(128));

        // 测试无效范围
        assertFalse(PresetControlEnum.isValidPresetNumber(0));   // 0号预留
        assertFalse(PresetControlEnum.isValidPresetNumber(256)); // 超出范围
        assertFalse(PresetControlEnum.isValidPresetNumber(-1));  // 负数
        assertFalse(PresetControlEnum.isValidPresetNumber(1000)); // 远超范围
    }

    @Test
    @DisplayName("预置位指令格式验证测试")
    void testPresetInstructionFormat() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x100)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 99)
                .build();

        // 验证指令格式符合A.3.4规范
        assertEquals((byte) 0xA5, instruction.getHeader());
        assertEquals((byte) 0x82, instruction.getInstructionCode());
        assertEquals((byte) 0x00, instruction.getData1()); // 字节5固定为00H
        assertEquals((byte) 99, instruction.getData2());   // 字节6为预置位号1-255
        assertEquals((byte) 0x00, instruction.getData3()); // 字节7高4位为0（预置位指令不使用）

        // 验证地址编码
        assertEquals(0x100, instruction.getFullAddress());
        assertEquals((byte) 0x00, instruction.getAddressLow());
        assertEquals((byte) 0x01, (instruction.getCombinationCode2() & 0x0F));

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("预置位指令边界值测试")
    void testPresetBoundaryValues() {
        // 测试最小有效预置位号
        PTZInstructionFormat minInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                .build();

        assertEquals((byte) 0x01, minInstruction.getData2());
        assertTrue(minInstruction.isValid());

        // 测试最大有效预置位号
        PTZInstructionFormat maxInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 255)
                .build();

        assertEquals((byte) 0xFF, maxInstruction.getData2());
        assertTrue(maxInstruction.isValid());

        // 测试无效预置位号应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 0)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 256)
                    .build();
        });
    }

    @Test
    @DisplayName("预置位指令序列化一致性测试")
    void testPresetSerializationConsistency() {
        PTZInstructionFormat original = PTZInstructionBuilder.create()
                .address(0x2FF)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, 177)
                .build();

        // 测试字节数组序列化
        byte[] bytes = original.toByteArray();
        PTZInstructionFormat fromBytes = PTZInstructionFormat.fromByteArray(bytes);
        assertEquals(original.getInstructionCode(), fromBytes.getInstructionCode());
        assertEquals(original.getData2(), fromBytes.getData2());
        assertEquals(original.getFullAddress(), fromBytes.getFullAddress());

        // 测试十六进制序列化
        String hex = original.toHexString();
        PTZInstructionFormat fromHex = PTZInstructionFormat.fromHexString(hex);
        assertEquals(original.getInstructionCode(), fromHex.getInstructionCode());
        assertEquals(original.getData2(), fromHex.getData2());
        assertEquals(original.getFullAddress(), fromHex.getFullAddress());

        // 验证所有版本都有效
        assertTrue(original.isValid());
        assertTrue(fromBytes.isValid());
        assertTrue(fromHex.isValid());
    }
}