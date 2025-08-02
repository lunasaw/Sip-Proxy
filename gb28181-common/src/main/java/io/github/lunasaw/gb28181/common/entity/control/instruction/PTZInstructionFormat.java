package io.github.lunasaw.gb28181.common.entity.control.instruction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PTZ指令格式基础类
 * 根据 A.3.1 指令格式 规范实现
 * <p>
 * 字节1: A5H (指令首字节)
 * 字节2: 组合码1 (高4位版本信息 + 低4位校验位)
 * 字节3: 地址低8位
 * 字节4: 指令码
 * 字节5: 数据1
 * 字节6: 数据2
 * 字节7: 组合码2 (高4位数据3 + 低4位地址高4位)
 * 字节8: 校验码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PTZInstructionFormat {

    /**
     * 指令首字节 固定为A5H
     */
    public static final byte INSTRUCTION_HEADER = (byte) 0xA5;

    /**
     * 版本信息 本标准版本1.0
     */
    public static final byte VERSION = 0x0;

    /**
     * 字节1: 指令首字节 A5H
     */
    private byte header = INSTRUCTION_HEADER;

    /**
     * 字节2: 组合码1
     */
    private byte combinationCode1;

    /**
     * 字节3: 地址低8位
     */
    private byte addressLow;

    /**
     * 字节4: 指令码
     */
    private byte instructionCode;

    /**
     * 字节5: 数据1
     */
    private byte data1;

    /**
     * 字节6: 数据2
     */
    private byte data2;

    /**
     * 字节7: 组合码2
     */
    private byte combinationCode2;

    /**
     * 字节8: 校验码
     */
    private byte checksum;

    /**
     * 构造函数
     *
     * @param address         设备地址 (0x000-0xFFF)
     * @param instructionCode 指令码
     * @param data1           数据1
     * @param data2           数据2
     * @param data3           数据3 (组合码2高4位)
     */
    public PTZInstructionFormat(int address, byte instructionCode, byte data1, byte data2, byte data3) {
        this.header = INSTRUCTION_HEADER;
        this.addressLow = (byte) (address & 0xFF);
        this.instructionCode = instructionCode;
        this.data1 = data1;
        this.data2 = data2;

        // 计算组合码1
        this.combinationCode1 = calculateCombinationCode1();

        // 计算组合码2
        this.combinationCode2 = (byte) (((data3 & 0x0F) << 4) | ((address >> 8) & 0x0F));

        // 计算校验码
        this.checksum = calculateChecksum();
    }

    /**
     * 计算组合码1
     * 校验位 = (字节1的高4位 + 字节1的低4位 + 字节2的高4位) % 16
     */
    private byte calculateCombinationCode1() {
        int headerHigh = (header >> 4) & 0x0F;
        int headerLow = header & 0x0F;
        int versionInfo = VERSION;

        int checkBit = (headerHigh + headerLow + versionInfo) % 16;
        return (byte) ((versionInfo << 4) | checkBit);
    }

    /**
     * 计算校验码
     * 字节8 = (字节1 + 字节2 + 字节3 + 字节4 + 字节5 + 字节6 + 字节7) % 256
     */
    private byte calculateChecksum() {
        int sum = (header & 0xFF) + (combinationCode1 & 0xFF) + (addressLow & 0xFF) +
                (instructionCode & 0xFF) + (data1 & 0xFF) + (data2 & 0xFF) + (combinationCode2 & 0xFF);
        return (byte) (sum % 256);
    }

    /**
     * 重新计算校验码
     */
    public void recalculateChecksum() {
        this.combinationCode1 = calculateCombinationCode1();
        this.checksum = calculateChecksum();
    }

    /**
     * 获取完整地址
     */
    public int getFullAddress() {
        return ((combinationCode2 & 0x0F) << 8) | (addressLow & 0xFF);
    }

    /**
     * 获取数据3
     */
    public byte getData3() {
        return (byte) ((combinationCode2 >> 4) & 0x0F);
    }

    /**
     * 转换为字节数组
     */
    public byte[] toByteArray() {
        return new byte[]{
                header, combinationCode1, addressLow, instructionCode,
                data1, data2, combinationCode2, checksum
        };
    }

    /**
     * 从字节数组创建指令
     */
    public static PTZInstructionFormat fromByteArray(byte[] bytes) {
        if (bytes == null || bytes.length != 8) {
            throw new IllegalArgumentException("指令字节数组长度必须为8");
        }

        PTZInstructionFormat instruction = new PTZInstructionFormat();
        instruction.header = bytes[0];
        instruction.combinationCode1 = bytes[1];
        instruction.addressLow = bytes[2];
        instruction.instructionCode = bytes[3];
        instruction.data1 = bytes[4];
        instruction.data2 = bytes[5];
        instruction.combinationCode2 = bytes[6];
        instruction.checksum = bytes[7];

        return instruction;
    }

    /**
     * 转换为十六进制字符串
     */
    public String toHexString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : toByteArray()) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * 从十六进制字符串解析
     */
    public static PTZInstructionFormat fromHexString(String hexString) {
        if (hexString == null || hexString.length() != 16) {
            throw new IllegalArgumentException("十六进制字符串长度必须为16");
        }

        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            String hex = hexString.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(hex, 16);
        }

        return fromByteArray(bytes);
    }

    /**
     * 验证指令格式是否正确
     */
    public boolean isValid() {
        // 验证首字节
        if (header != INSTRUCTION_HEADER) {
            return false;
        }

        // 验证校验码
        byte expectedChecksum = calculateChecksum();
        return checksum == expectedChecksum;
    }
}