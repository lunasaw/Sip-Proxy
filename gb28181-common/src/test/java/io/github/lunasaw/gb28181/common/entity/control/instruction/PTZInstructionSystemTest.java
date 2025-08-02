package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.crypto.PTZInstructionCrypto;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import io.github.lunasaw.gb28181.common.entity.control.instruction.manager.PTZInstructionManager;
import io.github.lunasaw.gb28181.common.entity.control.instruction.serializer.PTZInstructionSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PTZ指令系统单元测试
 */
class PTZInstructionSystemTest {

    private PTZInstructionFormat testInstruction;
    private final int testAddress = 0x123;
    private final byte testInstructionCode = 0x01;
    private final byte testData1 = 0x10;
    private final byte testData2 = 0x20;
    private final byte testData3 = 0x0F;

    @BeforeEach
    void setUp() {
        testInstruction = new PTZInstructionFormat(testAddress, testInstructionCode, testData1, testData2, testData3);
    }

    @Test
    @DisplayName("PTZ指令格式基础功能测试")
    void testPTZInstructionFormat() {
        // 测试基础属性
        assertEquals((byte) 0xA5, testInstruction.getHeader());
        assertEquals(testAddress, testInstruction.getFullAddress());
        assertEquals(testInstructionCode, testInstruction.getInstructionCode());
        assertEquals(testData1, testInstruction.getData1());
        assertEquals(testData2, testInstruction.getData2());
        assertEquals(testData3, testInstruction.getData3());

        // 测试字节数组转换
        byte[] bytes = testInstruction.toByteArray();
        assertEquals(8, bytes.length);
        assertEquals((byte) 0xA5, bytes[0]);

        // 测试十六进制字符串转换
        String hexString = testInstruction.toHexString();
        assertEquals(16, hexString.length());
        assertTrue(hexString.startsWith("A5"));

        // 测试指令有效性验证
        assertTrue(testInstruction.isValid());

        // 测试从字节数组重建
        PTZInstructionFormat rebuilt = PTZInstructionFormat.fromByteArray(bytes);
        assertEquals(testInstruction.getFullAddress(), rebuilt.getFullAddress());
        assertEquals(testInstruction.getInstructionCode(), rebuilt.getInstructionCode());

        // 测试从十六进制字符串重建
        PTZInstructionFormat fromHex = PTZInstructionFormat.fromHexString(hexString);
        assertEquals(testInstruction.getFullAddress(), fromHex.getFullAddress());
    }

    @Test
    @DisplayName("PTZ控制指令枚举测试")
    void testPTZControlEnum() {
        // 测试基础枚举查找
        assertEquals(PTZControlEnum.STOP, PTZControlEnum.getByCode((byte) 0x00));
        assertEquals(PTZControlEnum.PAN_RIGHT, PTZControlEnum.getByCode((byte) 0x01));
        assertEquals(PTZControlEnum.PAN_LEFT, PTZControlEnum.getByCode((byte) 0x02));
        assertEquals(PTZControlEnum.TILT_UP, PTZControlEnum.getByCode((byte) 0x08));

        // 测试方向检查
        assertTrue(PTZControlEnum.PAN_LEFT.hasPanControl());
        assertTrue(PTZControlEnum.TILT_UP.hasTiltControl());
        assertTrue(PTZControlEnum.ZOOM_IN.hasZoomControl());

        // 测试方向获取
        assertEquals(PTZControlEnum.PanDirection.LEFT, PTZControlEnum.PAN_LEFT.getPanDirection());
        assertEquals(PTZControlEnum.TiltDirection.UP, PTZControlEnum.TILT_UP.getTiltDirection());
        assertEquals(PTZControlEnum.ZoomDirection.IN, PTZControlEnum.ZOOM_IN.getZoomDirection());
    }

    @Test
    @DisplayName("FI控制指令枚举测试")
    void testFIControlEnum() {
        // 测试基础枚举查找
        assertEquals(FIControlEnum.STOP, FIControlEnum.getByCode((byte) 0x40));
        assertEquals(FIControlEnum.IRIS_OPEN, FIControlEnum.getByCode((byte) 0x44));
        assertEquals(FIControlEnum.FOCUS_NEAR, FIControlEnum.getByCode((byte) 0x42));

        // 测试控制类型检查
        assertTrue(FIControlEnum.IRIS_OPEN.hasIrisControl());
        assertTrue(FIControlEnum.FOCUS_NEAR.hasFocusControl());

        // 测试方向获取
        assertEquals(FIControlEnum.IrisDirection.OPEN, FIControlEnum.IRIS_OPEN.getIrisDirection());
        assertEquals(FIControlEnum.FocusDirection.NEAR, FIControlEnum.FOCUS_NEAR.getFocusDirection());
    }

    @Test
    @DisplayName("预置位控制指令枚举测试")
    void testPresetControlEnum() {
        // 测试基础枚举查找
        assertEquals(PresetControlEnum.SET_PRESET, PresetControlEnum.getByCode((byte) 0x81));
        assertEquals(PresetControlEnum.CALL_PRESET, PresetControlEnum.getByCode((byte) 0x82));
        assertEquals(PresetControlEnum.DELETE_PRESET, PresetControlEnum.getByCode((byte) 0x83));

        // 测试预置位号验证
        assertTrue(PresetControlEnum.isValidPresetNumber(1));
        assertTrue(PresetControlEnum.isValidPresetNumber(255));
        assertFalse(PresetControlEnum.isValidPresetNumber(0));
        assertFalse(PresetControlEnum.isValidPresetNumber(256));
    }

    @Test
    @DisplayName("Builder模式测试")
    void testPTZInstructionBuilder() {
        // 测试PTZ控制构建
        PTZInstructionFormat ptzInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .build();

        assertEquals(0x001, ptzInstruction.getFullAddress());
        assertEquals((byte) 0x01, ptzInstruction.getInstructionCode());
        assertEquals((byte) 0x40, ptzInstruction.getData1());
        assertEquals((byte) 0x20, ptzInstruction.getData2());

        // 测试FI控制构建
        PTZInstructionFormat fiInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .focusSpeed(0x80)
                .irisSpeed(0x60)
                .build();

        assertEquals(0x002, fiInstruction.getFullAddress());
        assertEquals((byte) 0x44, fiInstruction.getInstructionCode());

        // 测试预置位控制构建
        PTZInstructionFormat presetInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addPresetControl(PresetControlEnum.SET_PRESET, 10)
                .build();

        assertEquals(0x003, presetInstruction.getFullAddress());
        assertEquals((byte) 0x81, presetInstruction.getInstructionCode());
        assertEquals((byte) 0x00, presetInstruction.getData1());
        assertEquals((byte) 10, presetInstruction.getData2());

        // 测试组合PTZ控制
        PTZInstructionFormat combinedInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                        PTZControlEnum.TiltDirection.UP,
                        PTZControlEnum.ZoomDirection.IN)
                .horizontalSpeed(0x50)
                .verticalSpeed(0x30)
                .zoomSpeed(0x0A)
                .build();

        assertEquals((byte) 0x19, combinedInstruction.getInstructionCode()); // 0x01 + 0x08 + 0x10
    }

    @Test
    @DisplayName("序列化功能测试")
    void testSerialization() {
        // 测试字节数组序列化
        byte[] bytes = PTZInstructionSerializer.serializeToBytes(testInstruction);
        assertEquals(8, bytes.length);

        PTZInstructionFormat deserialized = PTZInstructionSerializer.deserializeFromBytes(bytes);
        assertEquals(testInstruction.getFullAddress(), deserialized.getFullAddress());

        // 测试十六进制序列化
        String hex = PTZInstructionSerializer.serializeToHex(testInstruction);
        assertEquals(16, hex.length());

        PTZInstructionFormat fromHex = PTZInstructionSerializer.deserializeFromHex(hex);
        assertEquals(testInstruction.getInstructionCode(), fromHex.getInstructionCode());

        // 测试Base64序列化
        String base64 = PTZInstructionSerializer.serializeToBase64(testInstruction);
        assertNotNull(base64);
        assertFalse(base64.isEmpty());

        PTZInstructionFormat fromBase64 = PTZInstructionSerializer.deserializeFromBase64(base64);
        assertEquals(testInstruction.getData1(), fromBase64.getData1());

        // 测试ByteBuffer序列化
        ByteBuffer buffer = PTZInstructionSerializer.serializeToByteBuffer(testInstruction);
        assertEquals(8, buffer.remaining());

        PTZInstructionFormat fromBuffer = PTZInstructionSerializer.deserializeFromByteBuffer(buffer);
        assertEquals(testInstruction.getData2(), fromBuffer.getData2());
    }

    @Test
    @DisplayName("加密解密功能测试")
    void testCrypto() {
        // 测试AES-GCM加密
        SecretKey aesKey = PTZInstructionCrypto.generateAESKey(256);
        assertNotNull(aesKey);

        byte[] encrypted = PTZInstructionCrypto.encryptAESGCM(testInstruction, aesKey);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 8); // 应该包含IV和认证标签

        PTZInstructionFormat decrypted = PTZInstructionCrypto.decryptAESGCM(encrypted, aesKey);
        assertEquals(testInstruction.getFullAddress(), decrypted.getFullAddress());
        assertEquals(testInstruction.getInstructionCode(), decrypted.getInstructionCode());

        // 测试XOR加密
        byte[] xorKey = PTZInstructionCrypto.generateRandomKey(8);
        byte[] xorEncrypted = PTZInstructionCrypto.encryptXOR(testInstruction, xorKey);
        assertEquals(8, xorEncrypted.length);

        PTZInstructionFormat xorDecrypted = PTZInstructionCrypto.decryptXOR(xorEncrypted, xorKey);
        assertEquals(testInstruction.getData3(), xorDecrypted.getData3());

        // 测试哈希计算
        byte[] md5Hash = PTZInstructionCrypto.calculateMD5Hash(testInstruction);
        assertEquals(16, md5Hash.length);

        byte[] sha256Hash = PTZInstructionCrypto.calculateSHA256Hash(testInstruction);
        assertEquals(32, sha256Hash.length);

        // 测试完整性验证
        assertTrue(PTZInstructionCrypto.verifyIntegrity(testInstruction, md5Hash, "MD5"));
        assertTrue(PTZInstructionCrypto.verifyIntegrity(testInstruction, sha256Hash, "SHA-256"));
    }

    @Test
    @DisplayName("指令管理器测试")
    void testInstructionManager() {
        // 测试指令类型识别
        assertEquals(PTZInstructionManager.InstructionType.PTZ_CONTROL,
                PTZInstructionManager.getInstructionType((byte) 0x01));
        assertEquals(PTZInstructionManager.InstructionType.FI_CONTROL,
                PTZInstructionManager.getInstructionType((byte) 0x40));
        assertEquals(PTZInstructionManager.InstructionType.PRESET_CONTROL,
                PTZInstructionManager.getInstructionType((byte) 0x81));

        // 测试枚举获取
        assertEquals(PTZControlEnum.PAN_RIGHT,
                PTZInstructionManager.getPTZControlEnum((byte) 0x01));
        assertEquals(FIControlEnum.STOP,
                PTZInstructionManager.getFIControlEnum((byte) 0x40));
        assertEquals(PresetControlEnum.SET_PRESET,
                PTZInstructionManager.getPresetControlEnum((byte) 0x81));

        // 测试指令码支持检查
        assertTrue(PTZInstructionManager.isSupportedInstructionCode((byte) 0x01));
        assertTrue(PTZInstructionManager.isSupportedInstructionCode((byte) 0x40));
        assertFalse(PTZInstructionManager.isSupportedInstructionCode((byte) 0xFF));

        // 测试描述信息获取
        String description = PTZInstructionManager.getInstructionDescription((byte) 0x01);
        assertNotNull(description);
        assertFalse(description.isEmpty());

        // 测试统计信息
        PTZInstructionManager.InstructionStatistics stats = PTZInstructionManager.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalCount() > 0);
        assertTrue(stats.getCountsByType().size() > 0);
    }

    @Test
    @DisplayName("错误处理测试")
    void testErrorHandling() {
        // 测试无效地址
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create().address(0x1000).build(); // 超出范围
        });

        // 测试无效预置位号
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionBuilder.create()
                    .address(0x001)
                    .addPresetControl(PresetControlEnum.SET_PRESET, 0) // 无效预置位号
                    .build();
        });

        // 测试无效字节数组
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromByteArray(new byte[]{0x01, 0x02}); // 长度不足
        });

        // 测试无效十六进制字符串
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionFormat.fromHexString("A5F0"); // 长度不足
        });

        // 测试XOR密钥长度
        assertThrows(IllegalArgumentException.class, () -> {
            PTZInstructionCrypto.encryptXOR(testInstruction, new byte[]{0x01}); // 密钥长度错误
        });
    }

    @Test
    @DisplayName("指令兼容性测试")
    void testInstructionCompatibility() {
        // 测试与现有PtzUtils的兼容性
        // 创建一个PTZ右移指令
        PTZInstructionFormat rightInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .zoomSpeed(0x0F)
                .build();

        // 验证指令格式符合规范
        assertTrue(rightInstruction.isValid());
        assertEquals((byte) 0xA5, rightInstruction.getHeader());
        assertEquals((byte) 0x01, rightInstruction.getInstructionCode()); // 右移指令码

        // 测试十六进制输出格式与现有系统兼容
        String hexString = rightInstruction.toHexString();
        assertTrue(hexString.startsWith("A5"));
        assertEquals(16, hexString.length());
    }
}