package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 巡航控制指令生成和解析完整测试
 * 验证所有巡航指令是否按照A.3.5规范正确生成
 */
class CruiseControlInstructionTest {

    @Test
    @DisplayName("加入巡航点指令测试")
    void testAddCruisePointInstruction() {
        // 测试向巡航组1添加预置位3
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 3)
                .build();

        assertEquals((byte) 0x84, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为巡航组号
        assertEquals((byte) 0x03, instruction.getData2()); // 字节6为预置位号
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试边界值：巡航组255，预置位255
        PTZInstructionFormat maxInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 255, 255)
                .build();

        assertEquals((byte) 0x84, maxInstruction.getInstructionCode());
        assertEquals((byte) 0xFF, maxInstruction.getData1());
        assertEquals((byte) 0xFF, maxInstruction.getData2());
        assertTrue(maxInstruction.isValid());
    }

    @Test
    @DisplayName("删除巡航点指令测试")
    void testDeleteCruisePointInstruction() {
        // 测试删除巡航组2的预置位5
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addCruiseControl(CruiseControlEnum.DELETE_CRUISE_POINT, 2, 5)
                .build();

        assertEquals((byte) 0x85, instruction.getInstructionCode());
        assertEquals((byte) 0x02, instruction.getData1()); // 字节5为巡航组号
        assertEquals((byte) 0x05, instruction.getData2()); // 字节6为预置位号
        assertTrue(instruction.isValid());

        // 测试删除整条巡航（字节6为00H）
        PTZInstructionFormat deleteEntireInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addCruiseControl(CruiseControlEnum.DELETE_CRUISE_POINT, 3, 0)
                .build();

        assertEquals((byte) 0x85, deleteEntireInstruction.getInstructionCode());
        assertEquals((byte) 0x03, deleteEntireInstruction.getData1());
        assertEquals((byte) 0x00, deleteEntireInstruction.getData2()); // 删除整条巡航
        assertTrue(deleteEntireInstruction.isValid());
    }

    @Test
    @DisplayName("设置巡航速度指令测试")
    void testSetCruiseSpeedInstruction() {
        // 测试设置巡航组1速度为100（数据分布：低8位=100，高4位=0）
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 100)
                .build();

        assertEquals((byte) 0x86, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为巡航组号
        assertEquals((byte) 100, instruction.getData2());  // 字节6为速度低8位
        assertEquals((byte) 0x00, instruction.getData3()); // 字节7高4位为速度高4位（0）
        assertTrue(instruction.isValid());

        // 测试大速度值：4095（0xFFF）= 低8位0xFF + 高4位0x0F
        PTZInstructionFormat maxSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 2, 1, 4095)
                .build();

        assertEquals((byte) 0x86, maxSpeedInstruction.getInstructionCode());
        assertEquals((byte) 0x02, maxSpeedInstruction.getData1());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData2()); // 4095 & 0xFF = 255
        assertEquals((byte) 0x0F, maxSpeedInstruction.getData3()); // (4095 >> 8) & 0x0F = 15
        assertTrue(maxSpeedInstruction.isValid());
    }

    @Test
    @DisplayName("设置巡航停留时间指令测试")
    void testSetCruiseStayTimeInstruction() {
        // 测试设置巡航组3停留时间为60秒
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x007)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_STAY_TIME, 3, 1, 60)
                .build();

        assertEquals((byte) 0x87, instruction.getInstructionCode());
        assertEquals((byte) 0x03, instruction.getData1()); // 字节5为巡航组号
        assertEquals((byte) 60, instruction.getData2());   // 字节6为时间低8位
        assertEquals((byte) 0x00, instruction.getData3()); // 字节7高4位为时间高4位
        assertTrue(instruction.isValid());

        // 测试大时间值：1800秒（0x708）= 低8位0x08 + 高4位0x07
        PTZInstructionFormat longTimeInstruction = PTZInstructionBuilder.create()
                .address(0x008)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_STAY_TIME, 4, 1, 1800)
                .build();

        assertEquals((byte) 0x87, longTimeInstruction.getInstructionCode());
        assertEquals((byte) 0x04, longTimeInstruction.getData1());
        assertEquals((byte) 0x08, longTimeInstruction.getData2()); // 1800 & 0xFF = 8
        assertEquals((byte) 0x07, longTimeInstruction.getData3()); // (1800 >> 8) & 0x0F = 7
        assertTrue(longTimeInstruction.isValid());
    }

    @Test
    @DisplayName("开始巡航指令测试")
    void testStartCruiseInstruction() {
        // 测试开始巡航组5
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x009)
                .addCruiseControl(CruiseControlEnum.START_CRUISE, 5)
                .build();

        assertEquals((byte) 0x88, instruction.getInstructionCode());
        assertEquals((byte) 0x05, instruction.getData1()); // 字节5为巡航组号
        assertEquals((byte) 0x00, instruction.getData2()); // 字节6为00H
        assertEquals(0x009, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试开始巡航组0
        PTZInstructionFormat zeroGroupInstruction = PTZInstructionBuilder.create()
                .address(0x00A)
                .addCruiseControl(CruiseControlEnum.START_CRUISE, 0)
                .build();

        assertEquals((byte) 0x88, zeroGroupInstruction.getInstructionCode());
        assertEquals((byte) 0x00, zeroGroupInstruction.getData1());
        assertEquals((byte) 0x00, zeroGroupInstruction.getData2());
        assertTrue(zeroGroupInstruction.isValid());
    }

    @ParameterizedTest
    @DisplayName("巡航组号范围测试")
    @ValueSource(ints = {0, 1, 50, 100, 200, 255})
    void testCruiseGroupNumberRange(int groupNumber) {
        // 测试有效巡航组号
        assertTrue(CruiseControlEnum.isValidGroupNumber(groupNumber));

        // 测试开始巡航
        PTZInstructionFormat startInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.START_CRUISE, groupNumber)
                .build();

        assertEquals((byte) 0x88, startInstruction.getInstructionCode());
        assertEquals((byte) groupNumber, startInstruction.getData1());
        assertTrue(startInstruction.isValid());
    }

    @ParameterizedTest
    @DisplayName("预置位号范围测试")
    @ValueSource(ints = {1, 25, 50, 100, 200, 255})
    void testPresetNumberRange(int presetNumber) {
        // 测试有效预置位号
        assertTrue(CruiseControlEnum.isValidPresetNumber(presetNumber));

        // 测试加入巡航点
        PTZInstructionFormat addInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, presetNumber)
                .build();

        assertEquals((byte) 0x84, addInstruction.getInstructionCode());
        assertEquals((byte) presetNumber, addInstruction.getData2());
        assertTrue(addInstruction.isValid());
    }

    @Test
    @DisplayName("巡航指令解析测试")
    void testCruiseInstructionParsing() {
        // 创建设置巡航速度指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 10, 20, 500)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();
        assertEquals(8, bytes.length);

        // 验证字节数组内容
        assertEquals((byte) 0xA5, bytes[0]); // 首字节
        assertEquals((byte) 0x86, bytes[3]); // 指令码
        assertEquals((byte) 10, bytes[4]);   // 巡航组号
        assertEquals((byte) (500 & 0xFF), bytes[5]); // 速度低8位

        // 从字节数组重建指令
        PTZInstructionFormat parsedInstruction = PTZInstructionFormat.fromByteArray(bytes);

        // 验证解析结果
        assertEquals(originalInstruction.getFullAddress(), parsedInstruction.getFullAddress());
        assertEquals(originalInstruction.getInstructionCode(), parsedInstruction.getInstructionCode());
        assertEquals(originalInstruction.getData1(), parsedInstruction.getData1());
        assertEquals(originalInstruction.getData2(), parsedInstruction.getData2());
        assertEquals(originalInstruction.getData3(), parsedInstruction.getData3());
        assertTrue(parsedInstruction.isValid());

        // 验证十六进制字符串解析
        String hexString = originalInstruction.toHexString();
        PTZInstructionFormat parsedFromHex = PTZInstructionFormat.fromHexString(hexString);
        assertEquals(originalInstruction.getInstructionCode(), parsedFromHex.getInstructionCode());
        assertTrue(parsedFromHex.isValid());
    }

    @Test
    @DisplayName("巡航指令枚举映射测试")
    void testCruiseEnumMapping() {
        // 测试指令码到枚举的映射
        assertEquals(CruiseControlEnum.ADD_CRUISE_POINT,
                CruiseControlEnum.getByCode((byte) 0x84));
        assertEquals(CruiseControlEnum.DELETE_CRUISE_POINT,
                CruiseControlEnum.getByCode((byte) 0x85));
        assertEquals(CruiseControlEnum.SET_CRUISE_SPEED,
                CruiseControlEnum.getByCode((byte) 0x86));
        assertEquals(CruiseControlEnum.SET_CRUISE_STAY_TIME,
                CruiseControlEnum.getByCode((byte) 0x87));
        assertEquals(CruiseControlEnum.START_CRUISE,
                CruiseControlEnum.getByCode((byte) 0x88));

        // 测试名称到枚举的映射
        assertEquals(CruiseControlEnum.ADD_CRUISE_POINT,
                CruiseControlEnum.getByName("加入巡航点"));
        assertEquals(CruiseControlEnum.DELETE_CRUISE_POINT,
                CruiseControlEnum.getByName("删除巡航点"));
        assertEquals(CruiseControlEnum.START_CRUISE,
                CruiseControlEnum.getByName("开始巡航"));

        // 测试无效映射
        assertNull(CruiseControlEnum.getByCode((byte) 0x83));
        assertNull(CruiseControlEnum.getByCode((byte) 0x89));
        assertNull(CruiseControlEnum.getByName("无效指令"));
    }

    @Test
    @DisplayName("巡航参数验证测试")
    void testCruiseParameterValidation() {
        // 测试有效参数范围
        assertTrue(CruiseControlEnum.isValidGroupNumber(0));
        assertTrue(CruiseControlEnum.isValidGroupNumber(255));
        assertTrue(CruiseControlEnum.isValidPresetNumber(1));
        assertTrue(CruiseControlEnum.isValidPresetNumber(255));
        assertTrue(CruiseControlEnum.isValidSpeed(0));
        assertTrue(CruiseControlEnum.isValidSpeed(4095));
        assertTrue(CruiseControlEnum.isValidStayTime(0));
        assertTrue(CruiseControlEnum.isValidStayTime(4095));

        // 测试无效参数范围
        assertFalse(CruiseControlEnum.isValidGroupNumber(-1));
        assertFalse(CruiseControlEnum.isValidGroupNumber(256));
        assertFalse(CruiseControlEnum.isValidPresetNumber(0));
        assertFalse(CruiseControlEnum.isValidPresetNumber(256));
        assertFalse(CruiseControlEnum.isValidSpeed(-1));
        assertFalse(CruiseControlEnum.isValidSpeed(4096));
        assertFalse(CruiseControlEnum.isValidStayTime(-1));
        assertFalse(CruiseControlEnum.isValidStayTime(4096));
    }

    @Test
    @DisplayName("巡航指令数据编码测试")
    void testCruiseDataEncoding() {
        // 测试12位数据的编码（速度和时间）
        int[] testValues = {0, 1, 255, 256, 1023, 2048, 4095};

        for (int value : testValues) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, value)
                    .build();

            // 验证数据编码
            byte expectedLow = (byte) (value & 0xFF);
            byte expectedHigh = (byte) ((value >> 8) & 0x0F);

            assertEquals(expectedLow, instruction.getData2());
            assertEquals(expectedHigh, instruction.getData3());
            assertTrue(instruction.isValid());

            // 验证数据解码
            int decodedValue = (instruction.getData2() & 0xFF) |
                    ((instruction.getData3() & 0x0F) << 8);
            assertEquals(value, decodedValue);
        }
    }

    @Test
    @DisplayName("巡航指令异常处理测试")
    void testCruiseInstructionExceptions() {
        // 测试无效巡航组号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.START_CRUISE, -1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.START_CRUISE, 256)
                    .build();
        });

        // 测试无效预置位号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 0)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 256)
                    .build();
        });

        // 测试无效速度或时间
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, -1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 4096)
                    .build();
        });
    }
}