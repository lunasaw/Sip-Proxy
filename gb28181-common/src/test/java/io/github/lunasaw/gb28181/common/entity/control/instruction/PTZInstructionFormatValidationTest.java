package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PTZ指令格式验证完整测试
 * 验证指令格式是否严格按照A.3.1规范实现
 */
class PTZInstructionFormatValidationTest {

    @Test
    @DisplayName("指令首字节验证测试")
    void testInstructionHeader() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        // 验证首字节固定为A5H
        assertEquals((byte) 0xA5, instruction.getHeader());
        assertEquals(PTZInstructionFormat.INSTRUCTION_HEADER, instruction.getHeader());

        // 验证字节数组第一个字节
        byte[] bytes = instruction.toByteArray();
        assertEquals((byte) 0xA5, bytes[0]);

        // 验证十六进制字符串以A5开头
        String hexString = instruction.toHexString();
        assertTrue(hexString.startsWith("A5"));
    }

    @Test
    @DisplayName("组合码1验证测试")
    void testCombinationCode1() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .build();

        // 验证版本信息为0（本标准版本1.0）
        assertEquals(PTZInstructionFormat.VERSION, 0x0);

        // 手动计算校验位验证
        byte header = instruction.getHeader();
        int headerHigh = (header >> 4) & 0x0F; // A5H的高4位 = AH = 10
        int headerLow = header & 0x0F;         // A5H的低4位 = 5H = 5
        int versionInfo = PTZInstructionFormat.VERSION; // 版本信息 = 0

        int expectedCheckBit = (headerHigh + headerLow + versionInfo) % 16; // (10 + 5 + 0) % 16 = 15 = FH

        byte combinationCode1 = instruction.getCombinationCode1();
        int actualVersionInfo = (combinationCode1 >> 4) & 0x0F;
        int actualCheckBit = combinationCode1 & 0x0F;

        assertEquals(versionInfo, actualVersionInfo);
        assertEquals(expectedCheckBit, actualCheckBit);
    }

    @ParameterizedTest
    @DisplayName("地址编码验证测试")
    @ValueSource(ints = {0x000, 0x001, 0x123, 0x456, 0x789, 0xABC, 0xFFF})
    void testAddressEncoding(int address) {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(address)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        // 验证完整地址
        assertEquals(address, instruction.getFullAddress());

        // 验证地址低8位（字节3）
        byte expectedAddressLow = (byte) (address & 0xFF);
        assertEquals(expectedAddressLow, instruction.getAddressLow());

        // 验证地址高4位（字节7低4位）
        byte expectedAddressHigh = (byte) ((address >> 8) & 0x0F);
        assertEquals(expectedAddressHigh, (instruction.getCombinationCode2() & 0x0F));

        // 验证字节数组中的地址编码
        byte[] bytes = instruction.toByteArray();
        assertEquals(expectedAddressLow, bytes[2]); // 字节3
        assertEquals(expectedAddressHigh, (bytes[6] & 0x0F)); // 字节7低4位

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("指令码验证测试")
    void testInstructionCode() {
        // 测试各种指令码
        PTZControlEnum[] ptzControls = {
                PTZControlEnum.STOP, PTZControlEnum.PAN_LEFT, PTZControlEnum.PAN_RIGHT,
                PTZControlEnum.TILT_UP, PTZControlEnum.TILT_DOWN, PTZControlEnum.ZOOM_IN, PTZControlEnum.ZOOM_OUT
        };

        for (PTZControlEnum control : ptzControls) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(control)
                    .build();

            assertEquals(control.getInstructionCode(), instruction.getInstructionCode());

            // 验证字节数组中的指令码（字节4）
            byte[] bytes = instruction.toByteArray();
            assertEquals(control.getInstructionCode(), bytes[3]);

            assertTrue(instruction.isValid());
        }

        // 测试FI指令码
        FIControlEnum[] fiControls = {
                FIControlEnum.STOP, FIControlEnum.IRIS_OPEN, FIControlEnum.IRIS_CLOSE,
                FIControlEnum.FOCUS_NEAR, FIControlEnum.FOCUS_FAR
        };

        for (FIControlEnum control : fiControls) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x002)
                    .addFIControl(control)
                    .build();

            assertEquals(control.getInstructionCode(), instruction.getInstructionCode());

            // 验证字节数组中的指令码（字节4）
            byte[] bytes = instruction.toByteArray();
            assertEquals(control.getInstructionCode(), bytes[3]);

            assertTrue(instruction.isValid());
        }
    }

    @Test
    @DisplayName("数据字节验证测试")
    void testDataBytes() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x80)
                .zoomSpeed(0x0F)
                .build();

        // 验证数据1（字节5）
        assertEquals((byte) 0x40, instruction.getData1());

        // 验证数据2（字节6）
        assertEquals((byte) 0x80, instruction.getData2());

        // 验证数据3（字节7高4位）
        assertEquals((byte) 0x0F, instruction.getData3());

        // 验证字节数组中的数据
        byte[] bytes = instruction.toByteArray();
        assertEquals((byte) 0x40, bytes[4]); // 字节5
        assertEquals((byte) 0x80, bytes[5]); // 字节6
        assertEquals((byte) 0x0F, (bytes[6] >> 4) & 0x0F); // 字节7高4位

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("组合码2验证测试")
    void testCombinationCode2() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x3A7) // 地址高4位=3, 低8位=A7
                .addPTZControl(PTZControlEnum.ZOOM_IN)
                .zoomSpeed(0x0C) // 数据3=C
                .build();

        // 验证组合码2 = (数据3 << 4) | 地址高4位 = (C << 4) | 3 = C3H
        byte expectedCombinationCode2 = (byte) ((0x0C << 4) | (0x3A7 >> 8));
        assertEquals(expectedCombinationCode2, instruction.getCombinationCode2());

        // 验证字节数组中的组合码2（字节7）
        byte[] bytes = instruction.toByteArray();
        assertEquals(expectedCombinationCode2, bytes[6]);

        // 验证数据3和地址高4位的提取
        assertEquals((byte) 0x0C, instruction.getData3());
        assertEquals(0x3A7, instruction.getFullAddress());

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("校验码计算验证测试")
    void testChecksumCalculation() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x80)
                .zoomSpeed(0x0F)
                .build();

        // 手动计算校验码
        byte[] bytes = instruction.toByteArray();
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (bytes[i] & 0xFF);
        }
        byte expectedChecksum = (byte) (sum % 256);

        // 验证校验码
        assertEquals(expectedChecksum, instruction.getChecksum());
        assertEquals(expectedChecksum, bytes[7]); // 字节8

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("指令长度验证测试")
    void testInstructionLength() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        // 验证字节数组长度固定为8
        byte[] bytes = instruction.toByteArray();
        assertEquals(8, bytes.length);

        // 验证十六进制字符串长度固定为16
        String hexString = instruction.toHexString();
        assertEquals(16, hexString.length());

        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("指令有效性验证测试")
    void testInstructionValidation() {
        // 创建有效指令
        PTZInstructionFormat validInstruction = PTZInstructionBuilder.create()
                .address(0x100)
                .addPTZControl(PTZControlEnum.PAN_LEFT)
                .horizontalSpeed(0x60)
                .build();

        assertTrue(validInstruction.isValid());

        // 测试首字节错误的情况
        byte[] invalidBytes = validInstruction.toByteArray();
        invalidBytes[0] = (byte) 0xB5; // 错误的首字节

        PTZInstructionFormat invalidInstruction = PTZInstructionFormat.fromByteArray(invalidBytes);
        assertFalse(invalidInstruction.isValid());

        // 测试校验码错误的情况
        byte[] wrongChecksumBytes = validInstruction.toByteArray();
        wrongChecksumBytes[7] = (byte) 0x00; // 错误的校验码

        PTZInstructionFormat wrongChecksumInstruction = PTZInstructionFormat.fromByteArray(wrongChecksumBytes);
        assertFalse(wrongChecksumInstruction.isValid());
    }

    @Test
    @DisplayName("指令重计算校验码测试")
    void testRecalculateChecksum() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x200)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .irisSpeed(0x90)
                .build();

        assertTrue(instruction.isValid());

        // 保存原始校验码
        byte originalChecksum = instruction.getChecksum();

        // 重新计算校验码
        instruction.recalculateChecksum();

        // 验证校验码未改变（因为指令本身没有修改）
        assertEquals(originalChecksum, instruction.getChecksum());
        assertTrue(instruction.isValid());
    }

    @Test
    @DisplayName("边界地址值验证测试")
    void testBoundaryAddressValues() {
        // 测试最小地址 0x000
        PTZInstructionFormat minAddressInstruction = PTZInstructionBuilder.create()
                .address(0x000)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        assertEquals(0x000, minAddressInstruction.getFullAddress());
        assertTrue(minAddressInstruction.isValid());

        // 测试最大地址 0xFFF
        PTZInstructionFormat maxAddressInstruction = PTZInstructionBuilder.create()
                .address(0xFFF)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        assertEquals(0xFFF, maxAddressInstruction.getFullAddress());
        assertTrue(maxAddressInstruction.isValid());

        // 验证地址编码
        assertEquals((byte) 0xFF, maxAddressInstruction.getAddressLow());
        assertEquals((byte) 0x0F, (maxAddressInstruction.getCombinationCode2() & 0x0F));
    }

    @Test
    @DisplayName("指令格式一致性验证测试")
    void testInstructionFormatConsistency() {
        // 创建各种类型的指令
        PTZInstructionFormat[] instructions = {
                // PTZ指令
                PTZInstructionBuilder.create().address(0x001).addPTZControl(PTZControlEnum.PAN_RIGHT).build(),

                // FI指令
                PTZInstructionBuilder.create().address(0x002).addFIControl(FIControlEnum.IRIS_OPEN).build(),

                // 预置位指令
                PTZInstructionBuilder.create().address(0x003).addPresetControl(PresetControlEnum.SET_PRESET, 10).build(),

                // 巡航指令
                PTZInstructionBuilder.create().address(0x004).addCruiseControl(CruiseControlEnum.START_CRUISE, 1).build(),

                // 扫描指令
                PTZInstructionBuilder.create().address(0x005).addScanControl(ScanControlEnum.START_AUTO_SCAN, 1, ScanControlEnum.ScanOperationType.START).build(),

                // 辅助开关指令
                PTZInstructionBuilder.create().address(0x006).addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1).build()
        };

        for (PTZInstructionFormat instruction : instructions) {
            // 验证所有指令都符合基本格式要求
            assertEquals((byte) 0xA5, instruction.getHeader());
            assertEquals(8, instruction.toByteArray().length);
            assertEquals(16, instruction.toHexString().length());
            assertTrue(instruction.isValid());

            // 验证序列化/反序列化一致性
            byte[] bytes = instruction.toByteArray();
            PTZInstructionFormat reconstructed = PTZInstructionFormat.fromByteArray(bytes);
            assertEquals(instruction.getFullAddress(), reconstructed.getFullAddress());
            assertEquals(instruction.getInstructionCode(), reconstructed.getInstructionCode());
            assertTrue(reconstructed.isValid());

            // 验证十六进制序列化/反序列化一致性
            String hex = instruction.toHexString();
            PTZInstructionFormat fromHex = PTZInstructionFormat.fromHexString(hex);
            assertEquals(instruction.getInstructionCode(), fromHex.getInstructionCode());
            assertTrue(fromHex.isValid());
        }
    }
}