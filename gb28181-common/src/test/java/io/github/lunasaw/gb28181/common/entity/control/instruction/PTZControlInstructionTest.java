package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PTZ控制指令生成和解析完整测试
 * 验证所有PTZ指令是否按照A.3.2规范正确生成
 */
class PTZControlInstructionTest {

    @Test
    @DisplayName("PTZ停止指令测试")
    void testPTZStopInstruction() {
        // 根据表A.5序号7：字节4=00H，PTZ的所有操作均停止
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .horizontalSpeed(0x00)
                .verticalSpeed(0x00)
                .zoomSpeed(0x0)
                .build();

        assertEquals((byte) 0x00, instruction.getInstructionCode());
        assertEquals(0x001, instruction.getFullAddress());
        assertTrue(instruction.isValid());

        // 验证生成的十六进制字符串
        String hexString = instruction.toHexString();
        assertEquals(16, hexString.length());
        assertTrue(hexString.startsWith("A5"));
    }

    @Test
    @DisplayName("PTZ水平方向控制测试")
    void testPTZHorizontalControl() {
        // 测试向左移动 - 表A.5序号5
        PTZInstructionFormat leftInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_LEFT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .zoomSpeed(0x0)
                .build();

        assertEquals((byte) 0x02, leftInstruction.getInstructionCode());
        assertEquals((byte) 0x40, leftInstruction.getData1()); // 水平速度
        assertEquals((byte) 0x20, leftInstruction.getData2()); // 垂直速度
        assertTrue(leftInstruction.isValid());

        // 测试向右移动 - 表A.5序号6
        PTZInstructionFormat rightInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x60)
                .verticalSpeed(0x30)
                .zoomSpeed(0x0)
                .build();

        assertEquals((byte) 0x01, rightInstruction.getInstructionCode());
        assertEquals((byte) 0x60, rightInstruction.getData1());
        assertEquals((byte) 0x30, rightInstruction.getData2());
        assertTrue(rightInstruction.isValid());
    }

    @Test
    @DisplayName("PTZ垂直方向控制测试")
    void testPTZVerticalControl() {
        // 测试向上移动 - 表A.5序号3
        PTZInstructionFormat upInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPTZControl(PTZControlEnum.TILT_UP)
                .horizontalSpeed(0x50)
                .verticalSpeed(0x80)
                .zoomSpeed(0x0)
                .build();

        assertEquals((byte) 0x08, upInstruction.getInstructionCode());
        assertEquals((byte) 0x50, upInstruction.getData1());
        assertEquals((byte) 0x80, upInstruction.getData2());
        assertTrue(upInstruction.isValid());

        // 测试向下移动 - 表A.5序号4
        PTZInstructionFormat downInstruction = PTZInstructionBuilder.create()
                .address(0x002)
                .addPTZControl(PTZControlEnum.TILT_DOWN)
                .horizontalSpeed(0x30)
                .verticalSpeed(0x70)
                .zoomSpeed(0x0)
                .build();

        assertEquals((byte) 0x04, downInstruction.getInstructionCode());
        assertEquals((byte) 0x30, downInstruction.getData1());
        assertEquals((byte) 0x70, downInstruction.getData2());
        assertTrue(downInstruction.isValid());
    }

    @Test
    @DisplayName("PTZ变倍控制测试")
    void testPTZZoomControl() {
        // 测试镜头放大 - 表A.5序号2
        PTZInstructionFormat zoomInInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addPTZControl(PTZControlEnum.ZOOM_IN)
                .horizontalSpeed(0x00)
                .verticalSpeed(0x00)
                .zoomSpeed(0x0F)
                .build();

        assertEquals((byte) 0x10, zoomInInstruction.getInstructionCode());
        assertEquals((byte) 0x0F, zoomInInstruction.getData3());
        assertTrue(zoomInInstruction.isValid());

        // 测试镜头缩小 - 表A.5序号1
        PTZInstructionFormat zoomOutInstruction = PTZInstructionBuilder.create()
                .address(0x003)
                .addPTZControl(PTZControlEnum.ZOOM_OUT)
                .horizontalSpeed(0x00)
                .verticalSpeed(0x00)
                .zoomSpeed(0x0A)
                .build();

        assertEquals((byte) 0x20, zoomOutInstruction.getInstructionCode());
        assertEquals((byte) 0x0A, zoomOutInstruction.getData3());
        assertTrue(zoomOutInstruction.isValid());
    }

    @Test
    @DisplayName("PTZ组合控制测试")
    void testPTZCombinedControl() {
        // 测试右上移动同时缩小 - 表A.5序号8示例
        PTZInstructionFormat combinedInstruction = PTZInstructionBuilder.create()
                .address(0x004)
                .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                        PTZControlEnum.TiltDirection.UP,
                        PTZControlEnum.ZoomDirection.OUT)
                .horizontalSpeed(0x50)
                .verticalSpeed(0x60)
                .zoomSpeed(0x08)
                .build();

        // 验证指令码：右(0x01) + 上(0x08) + 缩小(0x20) = 0x29
        assertEquals((byte) 0x29, combinedInstruction.getInstructionCode());
        assertEquals((byte) 0x50, combinedInstruction.getData1());
        assertEquals((byte) 0x60, combinedInstruction.getData2());
        assertEquals((byte) 0x08, combinedInstruction.getData3());
        assertTrue(combinedInstruction.isValid());

        // 验证方向检查
        assertTrue(combinedInstruction.getInstructionCode() != 0);
        PTZControlEnum controlEnum = PTZControlEnum.getByCode(combinedInstruction.getInstructionCode());
        if (controlEnum != null) {
            assertTrue(controlEnum.hasPanControl());
            assertTrue(controlEnum.hasTiltControl());
            assertTrue(controlEnum.hasZoomControl());
        }
    }

    @ParameterizedTest
    @DisplayName("PTZ指令码映射验证")
    @CsvSource({
            "0x00, STOP, 停止",
            "0x01, PAN_RIGHT, 向右",
            "0x02, PAN_LEFT, 向左",
            "0x04, TILT_DOWN, 向下",
            "0x08, TILT_UP, 向上",
            "0x10, ZOOM_IN, 放大",
            "0x20, ZOOM_OUT, 缩小"
    })
    void testPTZInstructionCodeMapping(String hexCode, String enumName, String description) {
        byte code = (byte) Integer.parseInt(hexCode.substring(2), 16);
        PTZControlEnum controlEnum = PTZControlEnum.getByCode(code);

        assertNotNull(controlEnum, "指令码 " + hexCode + " 应该有对应的枚举");
        assertEquals(enumName, controlEnum.name());
        assertEquals(description, controlEnum.getName());
    }

    @Test
    @DisplayName("PTZ指令解析测试")
    void testPTZInstructionParsing() {
        // 创建一个标准的右移指令
        PTZInstructionFormat originalInstruction = PTZInstructionBuilder.create()
                .address(0x123)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .zoomSpeed(0x0F)
                .build();

        // 序列化为字节数组
        byte[] bytes = originalInstruction.toByteArray();

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
    @DisplayName("PTZ指令速度范围测试")
    void testPTZSpeedRanges() {
        // 测试最小速度值
        PTZInstructionFormat minSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x00)
                .verticalSpeed(0x00)
                .zoomSpeed(0x0)
                .build();

        assertTrue(minSpeedInstruction.isValid());

        // 测试最大速度值
        PTZInstructionFormat maxSpeedInstruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_LEFT)
                .horizontalSpeed(0xFF)
                .verticalSpeed(0xFF)
                .zoomSpeed(0xF)
                .build();

        assertTrue(maxSpeedInstruction.isValid());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData1());
        assertEquals((byte) 0xFF, maxSpeedInstruction.getData2());
        assertEquals((byte) 0x0F, maxSpeedInstruction.getData3());
    }

    @Test
    @DisplayName("PTZ校验码计算验证")
    void testPTZChecksumCalculation() {
        PTZInstructionFormat instruction = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PAN_RIGHT)
                .horizontalSpeed(0x40)
                .verticalSpeed(0x20)
                .zoomSpeed(0x0F)
                .build();

        // 手动计算校验码进行验证
        byte[] bytes = instruction.toByteArray();
        int calculatedSum = 0;
        for (int i = 0; i < 7; i++) {
            calculatedSum += (bytes[i] & 0xFF);
        }
        byte expectedChecksum = (byte) (calculatedSum % 256);

        assertEquals(expectedChecksum, instruction.getChecksum());
        assertTrue(instruction.isValid());
    }
}