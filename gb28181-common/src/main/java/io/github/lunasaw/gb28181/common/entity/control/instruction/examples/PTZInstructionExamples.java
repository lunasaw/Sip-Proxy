package io.github.lunasaw.gb28181.common.entity.control.instruction.examples;

import io.github.lunasaw.gb28181.common.entity.control.instruction.PTZInstructionFormat;
import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.crypto.PTZInstructionCrypto;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;
import io.github.lunasaw.gb28181.common.entity.control.instruction.manager.PTZInstructionManager;
import io.github.lunasaw.gb28181.common.entity.control.instruction.serializer.PTZInstructionSerializer;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Set;

/**
 * PTZ指令系统使用示例
 * 展示如何使用所有组件构建、序列化、加密和管理PTZ指令
 */
@Slf4j
public class PTZInstructionExamples {

    /**
     * 基础PTZ控制示例
     */
    public static void basicPTZControlExample() {
        log.info("=== 基础PTZ控制示例 ===");

        // 使用Builder模式创建云台右移指令
        PTZInstructionFormat rightMoveInstruction = PTZInstructionBuilder.create()
                .address(0x001)                              // 设备地址
                .addPTZControl(PTZControlEnum.PAN_RIGHT)      // 右移控制
                .horizontalSpeed(0x40)                        // 水平速度
                .verticalSpeed(0x20)                          // 垂直速度
                .zoomSpeed(0x0F)                              // 变倍速度
                .build();

        log.info("右移指令: {}", rightMoveInstruction.toHexString());
        log.info("指令有效性: {}", rightMoveInstruction.isValid());

        // 使用组合控制创建右上移动+放大指令
        PTZInstructionFormat combinedInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                        PTZControlEnum.TiltDirection.UP,
                        PTZControlEnum.ZoomDirection.IN)
                .horizontalSpeed(0x60)
                .verticalSpeed(0x40)
                .zoomSpeed(0x08)
                .build();

        log.info("组合控制指令: {}", combinedInstruction.toHexString());

        // 停止所有PTZ动作
        PTZInstructionFormat stopInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .build();

        log.info("停止指令: {}", stopInstruction.toHexString());
    }

    /**
     * FI(聚焦/光圈)控制示例
     */
    public static void fiControlExample() {
        log.info("=== FI控制示例 ===");

        // 光圈放大指令
        PTZInstructionFormat irisOpenInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .focusSpeed(0x80)
                .irisSpeed(0x60)
                .build();

        log.info("光圈放大指令: {}", irisOpenInstruction.toHexString());

        // 聚焦近点指令
        PTZInstructionFormat focusNearInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addFIControl(FIControlEnum.FOCUS_NEAR)
                .focusSpeed(0x90)
                .build();

        log.info("聚焦近点指令: {}", focusNearInstruction.toHexString());

        // 组合FI控制：光圈缩小+聚焦远
        PTZInstructionFormat combinedFIInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addFIControl(FIControlEnum.IrisDirection.CLOSE,
                        FIControlEnum.FocusDirection.FAR)
                .focusSpeed(0x70)
                .irisSpeed(0x50)
                .build();

        log.info("组合FI控制指令: {}", combinedFIInstruction.toHexString());
    }

    /**
     * 预置位控制示例
     */
    public static void presetControlExample() {
        log.info("=== 预置位控制示例 ===");

        // 设置预置位1
        PTZInstructionFormat setPresetInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                .build();

        log.info("设置预置位1指令: {}", setPresetInstruction.toHexString());

        // 调用预置位5
        PTZInstructionFormat callPresetInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 5)
                .build();

        log.info("调用预置位5指令: {}", callPresetInstruction.toHexString());

        // 删除预置位10
        PTZInstructionFormat deletePresetInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, 10)
                .build();

        log.info("删除预置位10指令: {}", deletePresetInstruction.toHexString());
    }

    /**
     * 巡航控制示例
     */
    public static void cruiseControlExample() {
        log.info("=== 巡航控制示例 ===");

        // 向巡航组1添加预置位3
        PTZInstructionFormat addCruisePointInstruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 3)
                .build();

        log.info("添加巡航点指令: {}", addCruisePointInstruction.toHexString());

        // 设置巡航组1的速度为100
        PTZInstructionFormat setCruiseSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addCruiseControl(CruiseControlEnum.SET_CRUISE_SPEED, 1, 1, 100)
                .build();

        log.info("设置巡航速度指令: {}", setCruiseSpeedInstruction.toHexString());

        // 开始巡航组1
        PTZInstructionFormat startCruiseInstruction = PTZInstructionBuilder.create()
                .address(0x005)
                .addCruiseControl(CruiseControlEnum.START_CRUISE, 1)
                .build();

        log.info("开始巡航指令: {}", startCruiseInstruction.toHexString());
    }

    /**
     * 扫描控制示例
     */
    public static void scanControlExample() {
        log.info("=== 扫描控制示例 ===");

        // 设置扫描组1的左边界
        PTZInstructionFormat setLeftBoundaryInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addScanControl(ScanControlEnum.SET_LEFT_BOUNDARY, 1,
                        ScanControlEnum.ScanOperationType.SET_LEFT_BOUNDARY)
                .build();

        log.info("设置左边界指令: {}", setLeftBoundaryInstruction.toHexString());

        // 设置扫描组1的速度为200
        PTZInstructionFormat setScanSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addScanSpeedControl(1, 200)
                .build();

        log.info("设置扫描速度指令: {}", setScanSpeedInstruction.toHexString());

        // 开始自动扫描
        PTZInstructionFormat startScanInstruction = PTZInstructionBuilder.create()
                .address(0x006)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1,
                        ScanControlEnum.ScanOperationType.START)
                .build();

        log.info("开始扫描指令: {}", startScanInstruction.toHexString());
    }

    /**
     * 辅助开关控制示例
     */
    public static void auxiliaryControlExample() {
        log.info("=== 辅助开关控制示例 ===");

        // 开启雨刷(开关1)
        PTZInstructionFormat wiperOnInstruction = PTZInstructionBuilder.create()
                .address(0x007)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                .build();

        log.info("开启雨刷指令: {}", wiperOnInstruction.toHexString());

        // 关闭雨刷
        PTZInstructionFormat wiperOffInstruction = PTZInstructionBuilder.create()
                .address(0x007)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 1)
                .build();

        log.info("关闭雨刷指令: {}", wiperOffInstruction.toHexString());
    }

    /**
     * 序列化和反序列化示例
     */
    public static void serializationExample() {
        log.info("=== 序列化示例 ===");

        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x100)
                .addPTZControl(PTZControlEnum.PAN_LEFT)
                .horizontalSpeed(0x80)
                .build();

        // 十六进制序列化
        String hexString = PTZInstructionSerializer.serializeToHex(instruction);
        log.info("十六进制序列化: {}", hexString);

        // Base64序列化
        String base64String = PTZInstructionSerializer.serializeToBase64(instruction);
        log.info("Base64序列化: {}", base64String);

        // 反序列化验证
        PTZInstructionFormat deserializedFromHex = PTZInstructionSerializer.deserializeFromHex(hexString);
        PTZInstructionFormat deserializedFromBase64 = PTZInstructionSerializer.deserializeFromBase64(base64String);

        log.info("反序列化验证 - 十六进制: {}", deserializedFromHex.isValid());
        log.info("反序列化验证 - Base64: {}", deserializedFromBase64.isValid());
    }

    /**
     * 加密解密示例
     */
    public static void encryptionExample() {
        log.info("=== 加密解密示例 ===");

        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x200)
                .addFIControl(FIControlEnum.IRIS_OPEN)
                .irisSpeed(0xA0)
                .build();

        // AES-GCM加密
        SecretKey aesKey = PTZInstructionCrypto.generateAESKey(256);
        byte[] encrypted = PTZInstructionCrypto.encryptAESGCM(instruction, aesKey);
        log.info("AES-GCM加密数据长度: {} bytes", encrypted.length);

        PTZInstructionFormat decrypted = PTZInstructionCrypto.decryptAESGCM(encrypted, aesKey);
        log.info("AES-GCM解密验证: {}", decrypted.isValid());

        // XOR加密
        byte[] xorKey = PTZInstructionCrypto.generateRandomKey(8);
        byte[] xorEncrypted = PTZInstructionCrypto.encryptXOR(instruction, xorKey);
        log.info("XOR加密数据: {}", bytesToHex(xorEncrypted));

        PTZInstructionFormat xorDecrypted = PTZInstructionCrypto.decryptXOR(xorEncrypted, xorKey);
        log.info("XOR解密验证: {}", xorDecrypted.isValid());

        // 完整性验证
        byte[] hash = PTZInstructionCrypto.calculateSHA256Hash(instruction);
        boolean integrityOk = PTZInstructionCrypto.verifyIntegrity(instruction, hash, "SHA-256");
        log.info("完整性验证: {}", integrityOk);
    }

    /**
     * 指令管理器示例
     */
    public static void instructionManagerExample() {
        log.info("=== 指令管理器示例 ===");

        // 获取指令统计信息
        PTZInstructionManager.InstructionStatistics stats = PTZInstructionManager.getStatistics();
        log.info("指令统计信息:\\n{}", stats);

        // 检查指令支持情况
        byte[] testCodes = {(byte) 0x01, (byte) 0x40, (byte) 0x81, (byte) 0x89, (byte) 0xFF};
        for (byte code : testCodes) {
            boolean supported = PTZInstructionManager.isSupportedInstructionCode(code);
            String description = PTZInstructionManager.getInstructionDescription(code);
            log.info("指令码 0x{}: 支持={}, 描述={}",
                    String.format("%02X", code), supported, description);
        }

        // 获取各类型指令码
        for (PTZInstructionManager.InstructionType type : PTZInstructionManager.InstructionType.values()) {
            Set<Byte> codes = PTZInstructionManager.getInstructionCodesByType(type);
            log.info("{} 指令码数量: {}", type.getName(), codes.size());
        }
    }

    /**
     * 综合应用示例：完整的PTZ控制流程
     */
    public static void comprehensiveExample() {
        log.info("=== 综合应用示例 ===");

        try {
            // 1. 创建加密密钥
            SecretKey encryptionKey = PTZInstructionCrypto.generateAESKeyFromPassword("MySecretPassword123");

            // 2. 构建一系列PTZ控制指令
            PTZInstructionFormat[] instructions = {
                    // 设置预置位1
                    PTZInstructionBuilder.create()
                            .address(0x001)
                            .addPresetControl(PresetControlEnum.SET_PRESET, 1)
                            .build(),

                    // 云台移动到指定位置
                    PTZInstructionBuilder.create()
                            .address(0x001)
                            .addPTZControl(PTZControlEnum.PAN_RIGHT)
                            .horizontalSpeed(0x60)
                            .build(),

                    // 调用预置位1
                    PTZInstructionBuilder.create()
                            .address(0x001)
                            .addPresetControl(PresetControlEnum.CALL_PRESET, 1)
                            .build()
            };

            // 3. 序列化、加密并发送指令
            for (int i = 0; i < instructions.length; i++) {
                PTZInstructionFormat instruction = instructions[i];

                // 验证指令有效性
                if (!instruction.isValid()) {
                    log.error("指令{}无效", i + 1);
                    continue;
                }

                // 序列化为十六进制
                String hexString = PTZInstructionSerializer.serializeToHex(instruction);

                // 加密指令
                byte[] encrypted = PTZInstructionCrypto.encryptAESGCM(instruction, encryptionKey);
                String encryptedBase64 = PTZInstructionSerializer.serializeToBase64(
                        PTZInstructionFormat.fromByteArray(encrypted));

                // 获取指令描述
                String description = PTZInstructionManager.getInstructionDescription(
                        instruction.getInstructionCode());

                log.info("指令{}: {} - 原始={}, 加密={}",
                        i + 1, description, hexString, encryptedBase64.substring(0, 16) + "...");

                // 模拟发送延迟
                Thread.sleep(100);
            }

            log.info("综合示例执行完成");

        } catch (Exception e) {
            log.error("综合示例执行失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串工具方法
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 主方法：运行所有示例
     */
    public static void main(String[] args) {
        log.info("开始PTZ指令系统示例演示");

        basicPTZControlExample();
        fiControlExample();
        presetControlExample();
        cruiseControlExample();
        scanControlExample();
        auxiliaryControlExample();
        serializationExample();
        encryptionExample();
        instructionManagerExample();
        comprehensiveExample();

        log.info("PTZ指令系统示例演示完成");
    }
}