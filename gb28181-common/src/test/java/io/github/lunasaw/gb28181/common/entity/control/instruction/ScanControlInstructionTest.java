package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 扫描控制指令生成和解析完整测试
 * 验证所有扫描指令是否按照A.3.6规范正确生成
 */
class ScanControlInstructionTest {

    @Test
    @DisplayName("开始自动扫描指令测试")
    void testStartAutoScanInstruction() {
        // 测试开始扫描组1的自动扫描
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1,
                        ScanControlEnum.ScanOperationType.START)
                .build();

        assertEquals((byte) 0x89, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为扫描组号
        assertEquals((byte) 0x00, instruction.getData2()); // 字节6为操作类型（开始=00H）
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 测试扫描组255
        PTZInstructionFormat maxGroupInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, 255,
                        ScanControlEnum.ScanOperationType.START)
                .build();

        assertEquals((byte) 0x89, maxGroupInstruction.getInstructionCode());
        assertEquals((byte) 0xFF, maxGroupInstruction.getData1());
        assertEquals((byte) 0x00, maxGroupInstruction.getData2());
        assertTrue(maxGroupInstruction.isValid());
    }

    @Test
    @DisplayName("设置扫描边界指令测试")
    void testSetScanBoundaryInstruction() {
        // 测试设置左边界
        PTZInstructionFormat leftBoundaryInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addScanControl(ScanControlEnum.SET_LEFT_BOUNDARY, 2,
                        ScanControlEnum.ScanOperationType.SET_LEFT_BOUNDARY)
                .build();

        assertEquals((byte) 0x89, leftBoundaryInstruction.getInstructionCode());
        assertEquals((byte) 0x02, leftBoundaryInstruction.getData1()); // 扫描组号
        assertEquals((byte) 0x01, leftBoundaryInstruction.getData2()); // 操作类型（左边界=01H）
        assertTrue(leftBoundaryInstruction.isValid());

        // 测试设置右边界
        PTZInstructionFormat rightBoundaryInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addScanControl(ScanControlEnum.SET_RIGHT_BOUNDARY, 3,
                        ScanControlEnum.ScanOperationType.SET_RIGHT_BOUNDARY)
                .build();

        assertEquals((byte) 0x89, rightBoundaryInstruction.getInstructionCode());
        assertEquals((byte) 0x03, rightBoundaryInstruction.getData1()); // 扫描组号
        assertEquals((byte) 0x02, rightBoundaryInstruction.getData2()); // 操作类型（右边界=02H）
        assertTrue(rightBoundaryInstruction.isValid());
    }

    @Test
    @DisplayName("设置扫描速度指令测试")
    void testSetScanSpeedInstruction() {
        // 测试设置扫描组1速度为150
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addScanSpeedControl(1, 150)
                .build();

        assertEquals((byte) 0x8A, instruction.getInstructionCode());
        assertEquals((byte) 0x01, instruction.getData1()); // 字节5为扫描组号
        assertEquals((byte) 150, instruction.getData2());  // 字节6为速度低8位
        assertEquals((byte) 0x00, instruction.getData3()); // 字节7高4位为速度高4位（0）
        assertTrue(instruction.isValid());

        // 测试大速度值：2048（0x800）= 低8位0x00 + 高4位0x08
        PTZInstructionFormat highSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addScanSpeedControl(5, 2048)
                .build();

        assertEquals((byte) 0x8A, highSpeedInstruction.getInstructionCode());
        assertEquals((byte) 0x05, highSpeedInstruction.getData1());
        assertEquals((byte) 0x00, highSpeedInstruction.getData2()); // 2048 & 0xFF = 0
        assertEquals((byte) 0x08, highSpeedInstruction.getData3()); // (2048 >> 8) & 0x0F = 8
        assertTrue(highSpeedInstruction.isValid());

        // 测试最大速度值：4095（0xFFF）
        PTZInstructionFormat maxSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x007)
                .addScanSpeedControl(10, 4095)
                .build();

        assertEquals((byte) 0x8A, maxSpeedInstruction.getInstructionCode());
        assertEquals((byte) 0x0A, maxSpeedInstruction.getData1());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData2()); // 4095 & 0xFF = 255
        assertEquals((byte) 0x0F, maxSpeedInstruction.getData3()); // (4095 >> 8) & 0x0F = 15
        assertTrue(maxSpeedInstruction.isValid());
    }

    @ParameterizedTest
    @DisplayName("扫描组号范围测试")
    @ValueSource(ints = {0, 1, 50, 100, 200, 255})
    void testScanGroupNumberRange(int groupNumber) {
        // 测试有效扫描组号
        assertTrue(ScanControlEnum.isValidGroupNumber(groupNumber));

        // 测试开始扫描
        PTZInstructionFormat startInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, groupNumber,
                        ScanControlEnum.ScanOperationType.START)
                .build();

        assertEquals((byte) 0x89, startInstruction.getInstructionCode());
        assertEquals((byte) groupNumber, startInstruction.getData1());
        assertTrue(startInstruction.isValid());

        // 测试设置扫描速度
        PTZInstructionFormat speedInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addScanSpeedControl(groupNumber, 100)
                .build();

        assertEquals((byte) 0x8A, speedInstruction.getInstructionCode());
        assertEquals((byte) groupNumber, speedInstruction.getData1());
        assertTrue(speedInstruction.isValid());
    }

    @Test
    @DisplayName("扫描操作类型测试")
    void testScanOperationTypes() {
        // 测试所有操作类型
        ScanControlEnum.ScanOperationType[] types = {
                ScanControlEnum.ScanOperationType.START,
                ScanControlEnum.ScanOperationType.SET_LEFT_BOUNDARY,
                ScanControlEnum.ScanOperationType.SET_RIGHT_BOUNDARY
        };

        int[] expectedValues = {0x00, 0x01, 0x02};

        for (int i = 0; i < types.length; i++) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1, types[i])
                    .build();

            assertEquals((byte) expectedValues[i], instruction.getData2());
            assertTrue(instruction.isValid());

            // 测试反向查找
            assertEquals(types[i], ScanControlEnum.ScanOperationType.getByValue(expectedValues[i]));
        }

        // 测试无效操作类型
        assertNull(ScanControlEnum.ScanOperationType.getByValue(0x03));
        assertNull(ScanControlEnum.ScanOperationType.getByValue(-1));
    }

    @Test
    @DisplayName("扫描指令解析测试")
    void testScanInstructionParsing() {
        // 创建设置扫描速度指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addScanSpeedControl(20, 777)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();
        assertEquals(8, bytes.length);

        // 验证字节数组内容
        assertEquals((byte) 0xA5, bytes[0]); // 首字节
        assertEquals((byte) 0x8A, bytes[3]); // 指令码
        assertEquals((byte) 20, bytes[4]);   // 扫描组号
        assertEquals((byte) (777 & 0xFF), bytes[5]); // 速度低8位

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
    @DisplayName("扫描指令枚举映射测试")
    void testScanEnumMapping() {
        // 测试指令码到枚举的映射（注意：START_AUTO_SCAN、SET_LEFT_BOUNDARY、SET_RIGHT_BOUNDARY都使用0x89）
        assertEquals(ScanControlEnum.START_AUTO_SCAN,
                ScanControlEnum.getByCode((byte) 0x89));
        assertEquals(ScanControlEnum.SET_SCAN_SPEED,
                ScanControlEnum.getByCode((byte) 0x8A));

        // 测试名称到枚举的映射
        assertEquals(ScanControlEnum.START_AUTO_SCAN,
                ScanControlEnum.getByName("开始自动扫描"));
        assertEquals(ScanControlEnum.SET_LEFT_BOUNDARY,
                ScanControlEnum.getByName("设置左边界"));
        assertEquals(ScanControlEnum.SET_RIGHT_BOUNDARY,
                ScanControlEnum.getByName("设置右边界"));
        assertEquals(ScanControlEnum.SET_SCAN_SPEED,
                ScanControlEnum.getByName("设置扫描速度"));

        // 测试无效映射
        assertNull(ScanControlEnum.getByCode((byte) 0x8B));
        assertNull(ScanControlEnum.getByName("无效指令"));
    }

    @Test
    @DisplayName("扫描参数验证测试")
    void testScanParameterValidation() {
        // 测试有效参数范围
        assertTrue(ScanControlEnum.isValidGroupNumber(0));
        assertTrue(ScanControlEnum.isValidGroupNumber(255));
        assertTrue(ScanControlEnum.isValidSpeed(0));
        assertTrue(ScanControlEnum.isValidSpeed(4095));

        // 测试无效参数范围
        assertFalse(ScanControlEnum.isValidGroupNumber(-1));
        assertFalse(ScanControlEnum.isValidGroupNumber(256));
        assertFalse(ScanControlEnum.isValidSpeed(-1));
        assertFalse(ScanControlEnum.isValidSpeed(4096));
    }

    @Test
    @DisplayName("扫描速度数据编码测试")
    void testScanSpeedDataEncoding() {
        // 测试12位速度数据的编码
        int[] testSpeeds = {0, 1, 255, 256, 1024, 2048, 4095};

        for (int speed : testSpeeds) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanSpeedControl(1, speed)
                    .build();

            // 验证速度编码
            byte expectedLow = (byte) (speed & 0xFF);
            byte expectedHigh = (byte) ((speed >> 8) & 0x0F);

            assertEquals(expectedLow, instruction.getData2());
            assertEquals(expectedHigh, instruction.getData3());
            assertTrue(instruction.isValid());

            // 验证速度解码
            int decodedSpeed = (instruction.getData2() & 0xFF) |
                    ((instruction.getData3() & 0x0F) << 8);
            assertEquals(speed, decodedSpeed);
        }
    }

    @Test
    @DisplayName("扫描指令异常处理测试")
    void testScanInstructionExceptions() {
        // 测试无效扫描组号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanControl(ScanControlEnum.START_AUTO_SCAN, -1,
                            ScanControlEnum.ScanOperationType.START)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanControl(ScanControlEnum.START_AUTO_SCAN, 256,
                            ScanControlEnum.ScanOperationType.START)
                    .build();
        });

        // 测试无效速度
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanSpeedControl(1, -1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addScanSpeedControl(1, 4096)
                    .build();
        });
    }

    @Test
    @DisplayName("扫描指令组合使用测试")
    void testScanInstructionCombination() {
        // 模拟完整的扫描设置流程

        // 1. 设置左边界
        PTZInstructionFormat setLeftBoundary = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.SET_LEFT_BOUNDARY, 1,
                        ScanControlEnum.ScanOperationType.SET_LEFT_BOUNDARY)
                .build();

        // 2. 设置右边界
        PTZInstructionFormat setRightBoundary = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.SET_RIGHT_BOUNDARY, 1,
                        ScanControlEnum.ScanOperationType.SET_RIGHT_BOUNDARY)
                .build();

        // 3. 设置扫描速度
        PTZInstructionFormat setSpeed = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanSpeedControl(1, 300)
                .build();

        // 4. 开始扫描
        PTZInstructionFormat startScan = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1,
                        ScanControlEnum.ScanOperationType.START)
                .build();

        // 验证所有指令都有效
        assertTrue(setLeftBoundary.isValid());
        assertTrue(setRightBoundary.isValid());
        assertTrue(setSpeed.isValid());
        assertTrue(startScan.isValid());

        // 验证指令码
        assertEquals((byte) 0x89, setLeftBoundary.getInstructionCode());
        assertEquals((byte) 0x89, setRightBoundary.getInstructionCode());
        assertEquals((byte) 0x8A, setSpeed.getInstructionCode());
        assertEquals((byte) 0x89, startScan.getInstructionCode());

        // 验证操作类型区分
        assertEquals((byte) 0x01, setLeftBoundary.getData2());
        assertEquals((byte) 0x02, setRightBoundary.getData2());
        assertEquals((byte) 0x00, startScan.getData2());

        // 验证扫描组号一致性
        assertEquals((byte) 0x01, setLeftBoundary.getData1());
        assertEquals((byte) 0x01, setRightBoundary.getData1());
        assertEquals((byte) 0x01, setSpeed.getData1());
        assertEquals((byte) 0x01, startScan.getData1());
    }
}