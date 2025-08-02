package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import io.github.lunasaw.gb28181.common.entity.control.instruction.serializer.PTZInstructionSerializer;
import io.github.lunasaw.gb28181.common.entity.control.instruction.crypto.PTZInstructionCrypto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PTZ指令系统边界值和异常情况完整测试
 * 验证所有边界条件和异常处理
 */
class PTZInstructionBoundaryAndExceptionTest {

    @Test
    @DisplayName("地址边界值测试")
    void testAddressBoundaryValues() {
        // 测试最小有效地址
        PTZInstructionFormat minInstruction = PTZInstructionBuilder.create()
                .address(0x000)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        assertEquals(0x000, minInstruction.getFullAddress());
        assertTrue(minInstruction.isValid());

        // 测试最大有效地址
        PTZInstructionFormat maxInstruction = PTZInstructionBuilder.create()
                .address(0xFFF)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        assertEquals(0xFFF, maxInstruction.getFullAddress());
        assertTrue(maxInstruction.isValid());

        // 测试广播地址（0x000）
        PTZInstructionFormat broadcastInstruction = PTZInstructionBuilder.create()
                .address(0x000)
                .addPTZControl(PTZControlEnum.PAN_LEFT)
                .build();

        assertEquals(0x000, broadcastInstruction.getFullAddress());
        assertTrue(broadcastInstruction.isValid());
    }

    @Test
    @DisplayName("地址超出范围异常测试")
    void testAddressOutOfRangeExceptions() {
        // 测试负数地址
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(-1)
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
        });

        // 测试超出范围地址
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x1000) // 超出0xFFF
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0xFFFF) // 远超范围
                    .addPTZControl(PTZControlEnum.STOP)
                    .build();
        });
    }

    @ParameterizedTest
    @DisplayName("速度参数边界值测试")
    @ValueSource(ints = {0x00, 0x01, 0x7F, 0x80, 0xFE, 0xFF})
    void testSpeedBoundaryValues(int speed) {
        // 测试水平速度边界值
        PTZInstructionFormat horizontalSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(speed)
                .build();

        assertEquals((byte) speed, horizontalSpeedInstruction.getData1());
        assertTrue(horizontalSpeedInstruction.isValid());

        // 测试垂直速度边界值
        PTZInstructionFormat verticalSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPTZControl(PTZControlEnum.TILT_UP)
                .verticalSpeed(speed)
                .build();

        assertEquals((byte) speed, verticalSpeedInstruction.getData2());
        assertTrue(verticalSpeedInstruction.isValid());
    }

    @Test
    @DisplayName("速度参数异常测试")
    void testSpeedParameterExceptions() {
        // 测试水平速度超出范围
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(-1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_RIGHT)
                    .horizontalSpeed(0x100)
                    .build();
        });

        // 测试垂直速度超出范围
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.TILT_UP)
                    .verticalSpeed(-1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.TILT_UP)
                    .verticalSpeed(0x100)
                    .build();
        });

        // 测试变倍速度超出范围
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.ZOOM_IN)
                    .zoomSpeed(-1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(PTZControlEnum.ZOOM_IN)
                    .zoomSpeed(0x10)
                    .build();
        });
    }

    @Test
    @DisplayName("预置位号边界值和异常测试")
    void testPresetNumberBoundaryAndExceptions() {
        // 测试最小有效预置位号
        PTZInstructionFormat minPresetInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                .build();

        assertEquals((byte) 1, minPresetInstruction.getData2());
        assertTrue(minPresetInstruction.isValid());

        // 测试最大有效预置位号
        PTZInstructionFormat maxPresetInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 255)
                .build();

        assertEquals((byte) 0xFF, maxPresetInstruction.getData2());
        assertTrue(maxPresetInstruction.isValid());

        // 测试无效预置位号（0号预留）
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 0)
                    .build();
        });

        // 测试超出范围预置位号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 256)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, -1)
                    .build();
        });
    }

    @Test
    @DisplayName("12位数据边界值测试")
    void testTwelveBitDataBoundaryValues() {
        // 测试巡航速度最小值
        PTZInstructionFormat minSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 0)
                .build();

        assertEquals((byte) 0x00, minSpeedInstruction.getData2());
        assertEquals((byte) 0x00, minSpeedInstruction.getData3());
        assertTrue(minSpeedInstruction.isValid());

        // 测试巡航速度最大值（4095 = 0xFFF）
        PTZInstructionFormat maxSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 4095)
                .build();

        assertEquals((byte) 0xFF, maxSpeedInstruction.getData2()); // 低8位
        assertEquals((byte) 0x0F, maxSpeedInstruction.getData3()); // 高4位
        assertTrue(maxSpeedInstruction.isValid());

        // 测试扫描速度边界值
        PTZInstructionFormat scanSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addScanSpeedControl(1, 4095)
                .build();

        assertEquals((byte) 0xFF, scanSpeedInstruction.getData2());
        assertEquals((byte) 0x0F, scanSpeedInstruction.getData3());
        assertTrue(scanSpeedInstruction.isValid());
    }

    @Test
    @DisplayName("12位数据异常测试")
    void testTwelveBitDataExceptions() {
        // 测试巡航速度超出范围
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

        // 测试巡航停留时间超出范围
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_STAY_TIME, 1, 1, -1)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(CruiseControlEnum.SET_CRUISE_STAY_TIME, 1, 1, 4096)
                    .build();
        });

        // 测试扫描速度超出范围
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
    @DisplayName("序列化异常测试")
    void testSerializationExceptions() {
        // 测试空指令序列化
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.serializeToBytes(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.serializeToHex(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.serializeToBase64(null);
        });

        // 测试无效字节数组反序列化
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBytes(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBytes(new byte[]{});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBytes(new byte[]{0x01, 0x02}); // 长度不足
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBytes(new byte[10]); // 长度过长
        });

        // 测试无效十六进制字符串反序列化
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromHex(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromHex("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromHex("A5F0"); // 长度不足
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromHex("A5F001020304050607080910"); // 长度过长
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromHex("G5F00102030405060708"); // 无效字符
        });

        // 测试无效Base64字符串反序列化
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBase64(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBase64("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionSerializer.deserializeFromBase64("Invalid@Base64!");
        });
    }

    @Test
    @DisplayName("加密异常测试")
    void testEncryptionExceptions() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        // 测试空指令加密
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.encryptXOR(null, new byte[8]);
        });

        // 测试无效密钥长度
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.encryptXOR(instruction, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.encryptXOR(instruction, new byte[7]); // 密钥长度不足
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.encryptXOR(instruction, new byte[9]); // 密钥长度过长
        });

        // 测试XOR解密异常
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.decryptXOR(null, new byte[8]);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.decryptXOR(new byte[8], null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.decryptXOR(new byte[7], new byte[8]); // 数据长度不正确
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.decryptXOR(new byte[8], new byte[7]); // 密钥长度不正确
        });
    }

    @Test
    @DisplayName("指令格式异常测试")
    void testInstructionFormatExceptions() {
        // 测试从无效字节数组创建指令
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromByteArray(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromByteArray(new byte[]{});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromByteArray(new byte[7]); // 长度不足
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromByteArray(new byte[9]); // 长度过长
        });

        // 测试从无效十六进制字符串创建指令
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromHexString(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromHexString("A5F0010203"); // 长度不足
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromHexString("A5F0010203040506070809"); // 长度过长
        });
    }

    @Test
    @DisplayName("Builder模式空值异常测试")
    void testBuilderNullExceptions() {
        // 测试空控制枚举
        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl((PTZControlEnum) null)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addFIControl((FIControlEnum) null)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(null, 1)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addCruiseControl(null, 1)
                    .build();
        });

        // 测试空方向枚举
        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPTZControl(null, PTZControlEnum.TiltDirection.UP, PTZControlEnum.ZoomDirection.IN)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addFIControl(null, FIControlEnum.FocusDirection.NEAR)
                    .build();
        });
    }

    @Test
    @DisplayName("极端数据组合测试")
    void testExtremeDataCombinations() {
        // 测试所有最大值组合
        PTZInstructionFormat maxValuesInstruction = PTZInstructionBuilder.create()
                .address(0xFFF)
                .addPTZControl(PTZControlEnum.PAN_RIGHT_TILT_UP_ZOOM_OUT)
                .horizontalSpeed(0xFF)
                .verticalSpeed(0xFF)
                .zoomSpeed(0x0F)
                .build();

        assertEquals(0xFFF, maxValuesInstruction.getFullAddress());
        assertEquals((byte) 0xFF, maxValuesInstruction.getData1());
        assertEquals((byte) 0xFF, maxValuesInstruction.getData2());
        assertEquals((byte) 0x0F, maxValuesInstruction.getData3());
        assertTrue(maxValuesInstruction.isValid());

        // 测试所有最小值组合
        PTZInstructionFormat minValuesInstruction = PTZInstructionBuilder.create()
                .address(0x000)
                .addPTZControl(PTZControlEnum.STOP)
                .horizontalSpeed(0x00)
                .verticalSpeed(0x00)
                .zoomSpeed(0x00)
                .build();

        assertEquals(0x000, minValuesInstruction.getFullAddress());
        assertEquals((byte) 0x00, minValuesInstruction.getData1());
        assertEquals((byte) 0x00, minValuesInstruction.getData2());
        assertEquals((byte) 0x00, minValuesInstruction.getData3());
        assertTrue(minValuesInstruction.isValid());

        // 测试混合边界值
        PTZInstructionFormat mixedBoundaryInstruction = PTZInstructionBuilder.create()
                .address(0x800) // 中间值
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 255, 1, 4095)
                .build();

        assertEquals(0x800, mixedBoundaryInstruction.getFullAddress());
        assertEquals((byte) 0xFF, mixedBoundaryInstruction.getData1()); // 巡航组号最大值
        assertEquals((byte) 0xFF, mixedBoundaryInstruction.getData2()); // 速度低8位
        assertEquals((byte) 0x0F, mixedBoundaryInstruction.getData3()); // 速度高4位
        assertTrue(mixedBoundaryInstruction.isValid());
    }

    @Test
    @DisplayName("内存和性能边界测试")
    void testMemoryAndPerformanceBoundaries() {
        // 测试大量指令创建和序列化
        for (int i = 0; i < 1000; i++) {
            PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                    .address(i % 0x1000)
                    .addPTZControl(PTZControlEnum.values()[i % PTZControlEnum.values().length])
                    .horizontalSpeed(i % 256)
                    .verticalSpeed((i * 2) % 256)
                    .zoomSpeed((i * 3) % 16)
                    .build();

            assertTrue(instruction.isValid());

            // 测试序列化性能
            String hex = instruction.toHexString();
            assertEquals(16, hex.length());

            // 测试反序列化
            PTZInstructionFormat reconstructed = PTZInstructionFormat.fromHexString(hex);
            assertTrue(reconstructed.isValid());
            assertEquals(instruction.getInstructionCode(), reconstructed.getInstructionCode());
        }
    }
}