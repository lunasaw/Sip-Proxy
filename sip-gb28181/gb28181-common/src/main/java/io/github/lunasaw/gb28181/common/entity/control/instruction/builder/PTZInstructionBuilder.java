package io.github.lunasaw.gb28181.common.entity.control.instruction.builder;

import io.github.lunasaw.gb28181.common.entity.control.instruction.PTZInstructionFormat;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.*;

/**
 * PTZ指令构建器 - Builder设计模式实现
 * 提供流式API构建各种PTZ控制指令
 */
public class PTZInstructionBuilder {

    private int address = 0;
    private byte instructionCode = 0;
    private byte data1 = 0;
    private byte data2 = 0;
    private byte data3 = 0;

    /**
     * 创建构建器实例
     */
    public static PTZInstructionBuilder create() {
        return new PTZInstructionBuilder();
    }

    /**
     * 设置设备地址
     *
     * @param address 设备地址 (0x000-0xFFF)
     * @return this
     */
    public PTZInstructionBuilder address(int address) {
        if (address < 0 || address > 0xFFF) {
            throw new IllegalArgumentException("地址范围必须在0x000-0xFFF之间");
        }
        this.address = address;
        return this;
    }

    /**
     * 添加PTZ控制指令
     *
     * @param control PTZ控制类型
     * @return this
     */
    public PTZInstructionBuilder addPTZControl(PTZControlEnum control) {
        this.instructionCode = control.getInstructionCode();
        return this;
    }

    /**
     * 添加PTZ控制指令 - 支持组合控制
     *
     * @param pan  水平方向控制
     * @param tilt 垂直方向控制
     * @param zoom 变倍控制
     * @return this
     */
    public PTZInstructionBuilder addPTZControl(PTZControlEnum.PanDirection pan,
                                               PTZControlEnum.TiltDirection tilt,
                                               PTZControlEnum.ZoomDirection zoom) {
        byte code = 0;

        // 设置水平方向控制位
        switch (pan) {
            case LEFT:
                code |= 0x02;
                break;
            case RIGHT:
                code |= 0x01;
                break;
        }

        // 设置垂直方向控制位
        switch (tilt) {
            case UP:
                code |= 0x08;
                break;
            case DOWN:
                code |= 0x04;
                break;
        }

        // 设置变倍控制位
        switch (zoom) {
            case IN:
                code |= 0x10;
                break;
            case OUT:
                code |= 0x20;
                break;
        }

        this.instructionCode = code;
        return this;
    }

    /**
     * 添加FI控制指令
     *
     * @param control FI控制类型
     * @return this
     */
    public PTZInstructionBuilder addFIControl(FIControlEnum control) {
        this.instructionCode = control.getInstructionCode();
        return this;
    }

    /**
     * 添加FI控制指令 - 支持组合控制
     *
     * @param iris  光圈控制
     * @param focus 聚焦控制
     * @return this
     */
    public PTZInstructionBuilder addFIControl(FIControlEnum.IrisDirection iris,
                                              FIControlEnum.FocusDirection focus) {
        byte code = 0x40; // FI指令固定前缀

        // 设置光圈控制位
        switch (iris) {
            case OPEN:
                code |= 0x04;
                break;
            case CLOSE:
                code |= 0x08;
                break;
        }

        // 设置聚焦控制位
        switch (focus) {
            case NEAR:
                code |= 0x02;
                break;
            case FAR:
                code |= 0x01;
                break;
        }

        this.instructionCode = code;
        return this;
    }

    /**
     * 添加预置位控制指令
     *
     * @param control      预置位控制类型
     * @param presetNumber 预置位号 (1-255)
     * @return this
     */
    public PTZInstructionBuilder addPresetControl(PresetControlEnum control, int presetNumber) {
        if (!PresetControlEnum.isValidPresetNumber(presetNumber)) {
            throw new IllegalArgumentException("预置位号必须在1-255之间");
        }
        this.instructionCode = control.getInstructionCode();
        this.data1 = 0x00; // 字节5固定为00H
        this.data2 = (byte) presetNumber; // 字节6为预置位号
        return this;
    }

    /**
     * 添加巡航控制指令
     *
     * @param control     巡航控制类型
     * @param groupNumber 巡航组号 (0-255)
     * @return this
     */
    public PTZInstructionBuilder addCruiseControl(CruiseControlEnum control, int groupNumber) {
        if (!CruiseControlEnum.isValidGroupNumber(groupNumber)) {
            throw new IllegalArgumentException("巡航组号必须在0-255之间");
        }
        this.instructionCode = control.getInstructionCode();
        this.data1 = (byte) groupNumber; // 字节5为巡航组号
        return this;
    }

    /**
     * 添加巡航控制指令 - 带预置位号
     *
     * @param control      巡航控制类型
     * @param groupNumber  巡航组号 (0-255)
     * @param presetNumber 预置位号 (1-255)
     * @return this
     */
    public PTZInstructionBuilder addCruiseControl(CruiseControlEnum control, int groupNumber, int presetNumber) {
        addCruiseControl(control, groupNumber);
        if (!CruiseControlEnum.isValidPresetNumber(presetNumber)) {
            throw new IllegalArgumentException("预置位号必须在1-255之间");
        }
        this.data2 = (byte) presetNumber; // 字节6为预置位号
        return this;
    }

    /**
     * 添加巡航控制指令 - 带速度或时间数据
     *
     * @param control      巡航控制类型
     * @param groupNumber  巡航组号 (0-255)
     * @param presetNumber 预置位号 (1-255)
     * @param speedOrTime  速度或时间 (0-4095)
     * @return this
     */
    public PTZInstructionBuilder addCruiseControl(CruiseControlEnum control, int groupNumber,
                                                  int presetNumber, int speedOrTime) {
        addCruiseControl(control, groupNumber, presetNumber);
        if (!CruiseControlEnum.isValidSpeed(speedOrTime)) {
            throw new IllegalArgumentException("速度或时间数据必须在0-4095之间");
        }
        this.data2 = (byte) (speedOrTime & 0xFF); // 低8位
        this.data3 = (byte) ((speedOrTime >> 8) & 0x0F); // 高4位
        return this;
    }

    /**
     * 添加扫描控制指令
     *
     * @param control       扫描控制类型
     * @param groupNumber   扫描组号 (0-255)
     * @param operationType 操作类型
     * @return this
     */
    public PTZInstructionBuilder addScanControl(ScanControlEnum control, int groupNumber,
                                                ScanControlEnum.ScanOperationType operationType) {
        if (!ScanControlEnum.isValidGroupNumber(groupNumber)) {
            throw new IllegalArgumentException("扫描组号必须在0-255之间");
        }
        this.instructionCode = control.getInstructionCode();
        this.data1 = (byte) groupNumber; // 字节5为扫描组号
        this.data2 = (byte) operationType.getValue(); // 字节6为操作类型
        return this;
    }

    /**
     * 添加扫描速度控制指令
     *
     * @param groupNumber 扫描组号 (0-255)
     * @param speed       扫描速度 (0-4095)
     * @return this
     */
    public PTZInstructionBuilder addScanSpeedControl(int groupNumber, int speed) {
        if (!ScanControlEnum.isValidGroupNumber(groupNumber)) {
            throw new IllegalArgumentException("扫描组号必须在0-255之间");
        }
        if (!ScanControlEnum.isValidSpeed(speed)) {
            throw new IllegalArgumentException("扫描速度必须在0-4095之间");
        }
        this.instructionCode = ScanControlEnum.SET_SCAN_SPEED.getInstructionCode();
        this.data1 = (byte) groupNumber; // 字节5为扫描组号
        this.data2 = (byte) (speed & 0xFF); // 低8位
        this.data3 = (byte) ((speed >> 8) & 0x0F); // 高4位
        return this;
    }

    /**
     * 添加辅助开关控制指令
     *
     * @param control      辅助开关控制类型
     * @param switchNumber 开关编号 (0-255)
     * @return this
     */
    public PTZInstructionBuilder addAuxiliaryControl(AuxiliaryControlEnum control, int switchNumber) {
        if (!AuxiliaryControlEnum.isValidSwitchNumber(switchNumber)) {
            throw new IllegalArgumentException("开关编号必须在0-255之间");
        }
        this.instructionCode = control.getInstructionCode();
        this.data1 = (byte) switchNumber; // 字节5为开关编号
        return this;
    }

    /**
     * 设置水平控制速度
     *
     * @param speed 水平速度 (0x00-0xFF)
     * @return this
     */
    public PTZInstructionBuilder horizontalSpeed(int speed) {
        if (speed < 0 || speed > 0xFF) {
            throw new IllegalArgumentException("水平速度必须在0x00-0xFF之间");
        }
        this.data1 = (byte) speed;
        return this;
    }

    /**
     * 设置垂直控制速度
     *
     * @param speed 垂直速度 (0x00-0xFF)
     * @return this
     */
    public PTZInstructionBuilder verticalSpeed(int speed) {
        if (speed < 0 || speed > 0xFF) {
            throw new IllegalArgumentException("垂直速度必须在0x00-0xFF之间");
        }
        this.data2 = (byte) speed;
        return this;
    }

    /**
     * 设置变倍控制速度
     *
     * @param speed 变倍速度 (0x0-0xF)
     * @return this
     */
    public PTZInstructionBuilder zoomSpeed(int speed) {
        if (speed < 0 || speed > 0xF) {
            throw new IllegalArgumentException("变倍速度必须在0x0-0xF之间");
        }
        this.data3 = (byte) speed;
        return this;
    }

    /**
     * 设置聚焦速度
     *
     * @param speed 聚焦速度 (0x00-0xFF)
     * @return this
     */
    public PTZInstructionBuilder focusSpeed(int speed) {
        if (speed < 0 || speed > 0xFF) {
            throw new IllegalArgumentException("聚焦速度必须在0x00-0xFF之间");
        }
        this.data1 = (byte) speed;
        return this;
    }

    /**
     * 设置光圈速度
     *
     * @param speed 光圈速度 (0x00-0xFF)
     * @return this
     */
    public PTZInstructionBuilder irisSpeed(int speed) {
        if (speed < 0 || speed > 0xFF) {
            throw new IllegalArgumentException("光圈速度必须在0x00-0xFF之间");
        }
        this.data2 = (byte) speed;
        return this;
    }

    /**
     * 构建PTZ指令
     *
     * @return PTZ指令格式对象
     */
    public PTZInstructionFormat build() {
        return new PTZInstructionFormat(address, instructionCode, data1, data2, data3);
    }

    /**
     * 构建并转换为十六进制字符串
     *
     * @return 十六进制字符串
     */
    public String buildToHex() {
        return build().toHexString();
    }

    /**
     * 构建并转换为字节数组
     *
     * @return 字节数组
     */
    public byte[] buildToBytes() {
        return build().toByteArray();
    }
}