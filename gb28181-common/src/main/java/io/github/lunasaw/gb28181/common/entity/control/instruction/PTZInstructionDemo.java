package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.manager.PTZInstructionManager;
import io.github.lunasaw.gb28181.common.entity.control.instruction.serializer.PTZInstructionSerializer;

/**
 * PTZ指令系统演示
 */
public class PTZInstructionDemo {

    public static void main(String[] args) {
        System.out.println("=== PTZ指令系统演示 ===");

        // 1. 使用Builder模式创建PTZ控制指令
        PTZInstructionFormat rightMoveInstruction = PTZInstructionBuilder.create()
                .address(0x001)                              // 设备地址
                .addPTZControl(PTZControlEnum.PAN_RIGHT)      // 右移控制
                .horizontalSpeed(0x40)                        // 水平速度
                .verticalSpeed(0x20)                          // 垂直速度
                .zoomSpeed(0x0F)                              // 变倍速度
                .build();

        System.out.println("1. 右移指令: " + rightMoveInstruction.toHexString());
        System.out.println("   指令有效性: " + rightMoveInstruction.isValid());

        // 2. 序列化演示
        String hexString = PTZInstructionSerializer.serializeToHex(rightMoveInstruction);
        String base64String = PTZInstructionSerializer.serializeToBase64(rightMoveInstruction);

        System.out.println("2. 序列化结果:");
        System.out.println("   十六进制: " + hexString);
        System.out.println("   Base64: " + base64String);

        // 3. 反序列化验证
        PTZInstructionFormat deserialized = PTZInstructionSerializer.deserializeFromHex(hexString);
        System.out.println("3. 反序列化验证: " + deserialized.isValid());

        // 4. 指令管理器演示
        PTZInstructionManager.InstructionType type = PTZInstructionManager.getInstructionType((byte) 0x01);
        String description = PTZInstructionManager.getInstructionDescription((byte) 0x01);

        System.out.println("4. 指令管理:");
        System.out.println("   指令类型: " + (type != null ? type.getName() : "未知"));
        System.out.println("   指令描述: " + description);

        // 5. 统计信息
        PTZInstructionManager.InstructionStatistics stats = PTZInstructionManager.getStatistics();
        System.out.println("5. 系统统计:");
        System.out.println("   总指令数: " + stats.getTotalCount());

        System.out.println("\n=== 演示完成 ===");
    }
}