package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.AuxiliaryControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 辅助开关控制指令生成和解析完整测试
 * 验证所有辅助开关指令是否按照A.3.7规范正确生成
 */
class AuxiliaryControlInstructionTest {

    @Test
    @DisplayName("开关开启指令测试")
    void testSwitchOnInstruction() {
        // 测试开启雨刷（开关1）
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                .build();

        assertEquals((byte) 0x8C, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为辅助开关编号
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试开启灯光控制（开关2）
        PTZInstructionFormat lightOnInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 2)
                .build();

        assertEquals((byte) 0x8C, lightOnInstruction.getInstructionCode());
        assertEquals((byte) 0x02, lightOnInstruction.getData1());
        assertTrue(lightOnInstruction.isValid());

        // 测试最大开关编号255
        PTZInstructionFormat maxSwitchInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 255)
                .build();

        assertEquals((byte) 0x8C, maxSwitchInstruction.getInstructionCode());
        assertEquals((byte) 0xFF, maxSwitchInstruction.getData1());
        assertTrue(maxSwitchInstruction.isValid());
    }

    @Test
    @DisplayName("开关关闭指令测试")
    void testSwitchOffInstruction() {
        // 测试关闭雨刷（开关1）
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 1)
                .build();

        assertEquals((byte) 0x8D, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为辅助开关编号
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试关闭加热控制（开关3）
        PTZInstructionFormat heatingOffInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 3)
                .build();

        assertEquals((byte) 0x8D, heatingOffInstruction.getInstructionCode());
        assertEquals((byte) 0x03, heatingOffInstruction.getData1());
        assertTrue(heatingOffInstruction.isValid());

        // 测试最小开关编号0
        PTZInstructionFormat minSwitchInstruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 0)
                .build();

        assertEquals((byte) 0x8D, minSwitchInstruction.getInstructionCode());
        assertEquals((byte) 0x00, minSwitchInstruction.getData1());
        assertTrue(minSwitchInstruction.isValid());
    }

    @ParameterizedTest
    @DisplayName("辅助开关编号范围测试")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 50, 100, 200, 255})
    void testSwitchNumberRange(int switchNumber) {
        // 测试有效开关编号
        assertTrue(AuxiliaryControlEnum.isValidSwitchNumber(switchNumber));

        // 测试开关开启
        PTZInstructionFormat onInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, switchNumber)
                .build();

        assertEquals((byte) 0x8C, onInstruction.getInstructionCode());
        assertEquals((byte) switchNumber, onInstruction.getData1());
        assertTrue(onInstruction.isValid());

        // 测试开关关闭
        PTZInstructionFormat offInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, switchNumber)
                .build();

        assertEquals((byte) 0x8D, offInstruction.getInstructionCode());
        assertEquals((byte) switchNumber, offInstruction.getData1());
        assertTrue(offInstruction.isValid());
    }

    @Test
    @DisplayName("辅助开关类型测试")
    void testAuxiliarySwitchTypes() {
        // 测试预定义的开关类型
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.WIPER,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(1));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.LIGHT,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(2));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.HEATING,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(3));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.VENTILATION,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(4));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.DEFROST,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(5));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.CUSTOM,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(0));

        // 测试未定义类型返回CUSTOM
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.CUSTOM,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(100));
        assertEquals(AuxiliaryControlEnum.AuxiliarySwitchType.CUSTOM,
                AuxiliaryControlEnum.AuxiliarySwitchType.getByValue(-1));

        // 验证开关类型描述
        assertEquals("雨刷控制", AuxiliaryControlEnum.AuxiliarySwitchType.WIPER.getDescription());
        assertEquals("灯光控制", AuxiliaryControlEnum.AuxiliarySwitchType.LIGHT.getDescription());
        assertEquals("加热控制", AuxiliaryControlEnum.AuxiliarySwitchType.HEATING.getDescription());
        assertEquals("通风控制", AuxiliaryControlEnum.AuxiliarySwitchType.VENTILATION.getDescription());
        assertEquals("除霜控制", AuxiliaryControlEnum.AuxiliarySwitchType.DEFROST.getDescription());
        assertEquals("自定义控制", AuxiliaryControlEnum.AuxiliarySwitchType.CUSTOM.getDescription());
    }

    @Test
    @DisplayName("辅助开关指令解析测试")
    void testAuxiliaryInstructionParsing() {
        // 创建开启雨刷指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();
        assertEquals(8, bytes.length);

        // 验证字节数组内容
        assertEquals((byte) 0xA5, bytes[0]); // 首字节
        assertEquals((byte) 0x8C, bytes[3]); // 指令码
        assertEquals((byte) 0x01, bytes[4]); // 开关编号

        // 从字节数组重建指令
        PTZInstructionFormat parsedInstruction = PTZInstructionFormat.fromByteArray(bytes);

        // 验证解析结果
        assertEquals(originalInstruction.getFullAddress(), parsedInstruction.getFullAddress());
        assertEquals(originalInstruction.getInstructionCode(), parsedInstruction.getInstructionCode());
        assertEquals(originalInstruction.getData1(), parsedInstruction.getData1());
        assertTrue(parsedInstruction.isValid());

        // 验证十六进制字符串解析
        String hexString = originalInstruction.toHexString();
        PTZInstructionFormat parsedFromHex = PTZInstructionFormat.fromHexString(hexString);
        assertEquals(originalInstruction.getInstructionCode(), parsedFromHex.getInstructionCode());
        assertEquals((byte) 0x01, parsedFromHex.getData1());
        assertTrue(parsedFromHex.isValid());
    }

    @Test
    @DisplayName("辅助开关枚举映射测试")
    void testAuxiliaryEnumMapping() {
        // 测试指令码到枚举的映射
        assertEquals(AuxiliaryControlEnum.SWITCH_ON,
                AuxiliaryControlEnum.getByCode((byte) 0x8C));
        assertEquals(AuxiliaryControlEnum.SWITCH_OFF,
                AuxiliaryControlEnum.getByCode((byte) 0x8D));

        // 测试名称到枚举的映射
        assertEquals(AuxiliaryControlEnum.SWITCH_ON,
                AuxiliaryControlEnum.getByName("开关开"));
        assertEquals(AuxiliaryControlEnum.SWITCH_OFF,
                AuxiliaryControlEnum.getByName("开关关"));

        // 测试无效映射
        assertNull(AuxiliaryControlEnum.getByCode((byte) 0x8B));
        assertNull(AuxiliaryControlEnum.getByCode((byte) 0x8E));
        assertNull(AuxiliaryControlEnum.getByName("无效指令"));
    }

    @Test
    @DisplayName("辅助开关参数验证测试")
    void testAuxiliaryParameterValidation() {
        // 测试有效开关编号范围 0-255
        assertTrue(AuxiliaryControlEnum.isValidSwitchNumber(0));
        assertTrue(AuxiliaryControlEnum.isValidSwitchNumber(1));
        assertTrue(AuxiliaryControlEnum.isValidSwitchNumber(255));
        assertTrue(AuxiliaryControlEnum.isValidSwitchNumber(128));

        // 测试无效开关编号范围
        assertFalse(AuxiliaryControlEnum.isValidSwitchNumber(-1));
        assertFalse(AuxiliaryControlEnum.isValidSwitchNumber(256));
        assertFalse(AuxiliaryControlEnum.isValidSwitchNumber(1000));
    }

    @Test
    @DisplayName("辅助开关指令格式验证测试")
    void testAuxiliaryInstructionFormat() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x100)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 42)
                .build();

        // 验证指令格式符合A.3.7规范
        assertEquals((byte) 0xA5, instruction.getHeader());
        assertEquals((byte) 0x8C, instruction.getInstructionCode());
        assertEquals((byte) 42, instruction.getData1()); // 字节5为开关编号

        // 验证地址编码
        assertEquals(0x100, instruction.getFullAddress());
        assertEquals((byte) 0x00, instruction.getAddressLow());
        assertEquals((byte) 0x01, (instruction.getCombinationCode2() & 0x0F));

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("辅助开关指令异常处理测试")
    void testAuxiliaryInstructionExceptions() {
        // 测试无效开关编号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, -1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 256)
                    .build();
        });
    }

    @Test
    @DisplayName("辅助开关控制场景测试")
    void testAuxiliaryControlScenarios() {
        // 场景1：雨刷控制
        PTZInstructionFormat wiperOn = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                .build();

        PTZInstructionFormat wiperOff = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 1)
                .build();

        assertEquals((byte) 0x8C, wiperOn.getInstructionCode());
        assertEquals((byte) 0x8D, wiperOff.getInstructionCode());
        assertEquals((byte) 0x01, wiperOn.getData1());
        assertEquals((byte) 0x01, wiperOff.getData1());
        assertTrue(wiperOn.isValid());
        assertTrue(wiperOff.isValid());

        // 场景2：灯光控制
        PTZInstructionFormat lightOn = PTZInstructionBuilder.create()
                .address(0x002)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 2)
                .build();

        PTZInstructionFormat lightOff = PTZInstructionBuilder.create()
                .address(0x002)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 2)
                .build();

        assertEquals((byte) 0x8C, lightOn.getInstructionCode());
        assertEquals((byte) 0x8D, lightOff.getInstructionCode());
        assertEquals((byte) 0x02, lightOn.getData1());
        assertEquals((byte) 0x02, lightOff.getData1());
        assertTrue(lightOn.isValid());
        assertTrue(lightOff.isValid());

        // 场景3：多个辅助设备同时控制（需要发送多个指令）
        int[] devices = {1, 2, 3, 4, 5}; // 雨刷、灯光、加热、通风、除霜

        // 全部开启
        for (int device : devices) {
            PTZInstructionFormat onInstruction = PTZInstructionBuilder.create()
                    .address(0x003)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, device)
                    .build();

            assertEquals((byte) 0x8C, onInstruction.getInstructionCode());
            assertEquals((byte) device, onInstruction.getData1());
            assertTrue(onInstruction.isValid());
        }

        // 全部关闭
        for (int device : devices) {
            PTZInstructionFormat offInstruction = PTZInstructionBuilder.create()
                    .address(0x003)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, device)
                    .build();

            assertEquals((byte) 0x8D, offInstruction.getInstructionCode());
            assertEquals((byte) device, offInstruction.getData1());
            assertTrue(offInstruction.isValid());
        }
    }

    @Test
    @DisplayName("辅助开关指令序列化一致性测试")
    void testAuxiliarySerializationConsistency() {
        PTZInstructionFormat original = PTZInstructionBuilder.create()
                .address(0x3FF)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 199)
                .build();

        // 测试字节数组序列化
        byte[] bytes = original.toByteArray();
        PTZInstructionFormat fromBytes = PTZInstructionFormat.fromByteArray(bytes);
        assertEquals(original.getInstructionCode(), fromBytes.getInstructionCode());
        assertEquals(original.getData1(), fromBytes.getData1());
        assertEquals(original.getFullAddress(), fromBytes.getFullAddress());

        // 测试十六进制序列化
        String hex = original.toHexString();
        PTZInstructionFormat fromHex = PTZInstructionFormat.fromHexString(hex);
        assertEquals(original.getInstructionCode(), fromHex.getInstructionCode());
        assertEquals(original.getData1(), fromHex.getData1());
        assertEquals(original.getFullAddress(), fromHex.getFullAddress());

        // 验证所有版本都有效
        assertTrue(original.isValid());
        assertTrue(fromBytes.isValid());
        assertTrue(fromHex.isValid());
    }

    @Test
    @DisplayName("辅助开关指令功能描述验证")
    void testAuxiliaryFunctionDescription() {
        // 验证开关开指令描述包含开关量和模拟量的功能
        String onDescription = AuxiliaryControlEnum.SWITCH_ON.getDescription();
        assertTrue(onDescription.contains("开关开"));
        assertTrue(onDescription.contains("模拟量步进数值增加"));

        // 验证开关关指令描述包含开关量和模拟量的功能
        String offDescription = AuxiliaryControlEnum.SWITCH_OFF.getDescription();
        assertTrue(offDescription.contains("开关关"));
        assertTrue(offDescription.contains("模拟量步进数值减少"));
    }
}