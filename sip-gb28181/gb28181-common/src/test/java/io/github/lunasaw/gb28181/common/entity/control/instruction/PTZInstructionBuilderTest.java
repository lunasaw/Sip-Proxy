package io.github.lunasaw.gb28181.common.entity.control.instruction;

import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.AuxiliaryControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PresetControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GBT-28181-2022 §A.3.2-7 PTZ 指令构建器测试。
 *
 * <p>对照标准表 A.5/A.7/A.8/A.9/A.10/A.11 的「指令举例」逐节验证 hex 输出。
 * 8 字节中字节 1=A5、字节 2=0F、字节 3=01（地址低 8 位，本测试统一用 0x001）；字节 4-7
 * 由各子节构建器决定；字节 8 是前 7 字节算术和 mod 256。
 *
 * @author luna
 */
class PTZInstructionBuilderTest {

    @Nested
    @DisplayName("§A.3.2 PTZ 指令（表 A.5）")
    class PtzInstructions {

        @Test
        @DisplayName("表 A.5 序号 1：镜头变倍缩小，字节 4=20H，变焦速度 8（4 bit）→ 字节 7 高 4 位=0x80")
        void zoomOutAtSpeed8() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.ZOOM_OUT)
                .zoomSpeed(8)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("20");  // 字节 4
            assertThat(hex.substring(12, 14)).isEqualToIgnoringCase("80"); // 字节 7：高 4 位=zoom 速度，低 4 位=地址高 4 位（0）
            assertThat(PTZInstructionFormat.fromHexString(hex).isValid()).isTrue();
        }

        @Test
        @DisplayName("表 A.5 序号 3：云台向上，字节 4=08H + 字节 6 速度")
        void tiltUpAtSpeed100() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.TILT_UP)
                .verticalSpeed(100)
                .buildToHex();
            assertThat(hex.substring(0, 6)).isEqualTo("A50F01");
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("08");  // 字节 4
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("64"); // 字节 6 = 100 = 0x64
        }

        @Test
        @DisplayName("表 A.5 序号 7：PTZ 全停（字节 4 全 0）")
        void allStop() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.STOP)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("00");
        }

        @Test
        @DisplayName("表 A.5 序号 8：组合（右上 + 缩小），字节 4=29H")
        void rightUpZoomOutCombo() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                               PTZControlEnum.TiltDirection.UP,
                               PTZControlEnum.ZoomDirection.OUT)
                .horizontalSpeed(0x80)
                .verticalSpeed(0x80)
                .zoomSpeed(8)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("29");  // 0x20 | 0x08 | 0x01
            // 验证完整 hex 通过 8 字节格式校验
            assertThat(PTZInstructionFormat.fromHexString(hex).isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("§A.3.3 FI 指令（表 A.7）")
    class FiInstructions {

        @Test
        @DisplayName("表 A.7 序号 1：光圈缩小，字节 4=48H")
        void irisClose() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.IRIS_CLOSE)
                .irisSpeed(0x80)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("48");
        }

        @Test
        @DisplayName("表 A.7 序号 6：组合（光圈缩小 + 聚焦远），字节 4=49H")
        void irisCloseFocusFarCombo() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addFIControl(FIControlEnum.IrisDirection.CLOSE,
                              FIControlEnum.FocusDirection.FAR)
                .focusSpeed(0x80)
                .irisSpeed(0x80)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("49");  // 0x40 | 0x08 | 0x01
            assertThat(PTZInstructionFormat.fromHexString(hex).isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("§A.3.4 预置位指令（表 A.8）")
    class PresetInstructions {

        @Test
        @DisplayName("表 A.8 序号 1：设置预置位 5，字节 4=81H, 字节 6=05H")
        void setPreset5() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 5)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("81");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("00");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("05");
            assertThat(PTZInstructionFormat.fromHexString(hex).isValid()).isTrue();
        }

        @Test
        @DisplayName("表 A.8 序号 2：调用预置位 100，字节 4=82H, 字节 6=64H")
        void invokePreset100() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.CALL_PRESET, 100)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("82");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("64");
        }

        @Test
        @DisplayName("表 A.8 序号 3：删除预置位 255")
        void deletePreset255() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.DELETE_PRESET, 255)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("83");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("FF");
        }

        @Test
        @DisplayName("§A.3.4 边界：预置位号 0 拒绝（0 号预留）")
        void presetZeroRejected() {
            assertThatThrownBy(() -> PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("§A.3.4 边界：预置位号 256 拒绝（最大 255）")
        void preset256Rejected() {
            assertThatThrownBy(() -> PTZInstructionBuilder.create()
                .address(0x001)
                .addPresetControl(PresetControlEnum.SET_PRESET, 256))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("§A.3.5 ���航指令（表 A.9）")
    class CruiseInstructions {

        @Test
        @DisplayName("表 A.9 序号 1：加入巡航点（组 1，预置位 5），字节 4=84H, 字节 5=01H, 字节 6=05H")
        void addCruisePoint() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 5)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("84");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("05");
        }

        @Test
        @DisplayName("表 A.9 序号 5：开始巡航（组 1），字节 4=88H, 字节 6=00H")
        void startCruiseGroup1() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addCruiseControl(CruiseControlEnum.START_CRUISE, 1)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("88");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
        }
    }

    @Nested
    @DisplayName("§A.3.6 扫描指令（表 A.10）")
    class ScanInstructions {

        @Test
        @DisplayName("表 A.10 序号 1：开始自动扫描（组 1），字节 4=89H, 字节 5=01H, 字节 6=00H")
        void startAutoScan() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1, ScanControlEnum.ScanOperationType.START)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("89");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("00");
        }

        @Test
        @DisplayName("表 A.10 序号 4：设置自动扫描速度（组 1，速度 50）字节 4=8AH, 字节 5=01H, 字节 6=32H")
        void setScanSpeed() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addScanSpeedControl(1, 50)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("8A");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
            assertThat(hex.substring(10, 12)).isEqualToIgnoringCase("32");
        }
    }

    @Nested
    @DisplayName("§A.3.7 辅助开关（表 A.11）")
    class AuxiliaryInstructions {

        @Test
        @DisplayName("表 A.11 序号 1：开��开（雨刷=1），字节 4=8CH, 字节 5=01H")
        void switchOnWiper() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("8C");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
        }

        @Test
        @DisplayName("表 A.11 序号 2：开关关（雨刷=1），字节 4=8DH")
        void switchOffWiper() {
            String hex = PTZInstructionBuilder.create()
                .address(0x001)
                .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_OFF, 1)
                .buildToHex();
            assertThat(hex.substring(6, 8)).isEqualToIgnoringCase("8D");
            assertThat(hex.substring(8, 10)).isEqualToIgnoringCase("01");
        }
    }

    @Nested
    @DisplayName("§A.3.1 通用：所有 builder 输出必须通过 PTZInstructionFormat 校验")
    class FormatIntegrity {

        @Test
        @DisplayName("所有子节示例 hex 串都通过 isValid() 校验")
        void allBuiltHexShouldValidate() {
            String[] hexes = new String[]{
                PTZInstructionBuilder.create().address(0x001)
                    .addPTZControl(PTZControlEnum.PAN_LEFT).horizontalSpeed(100).buildToHex(),
                PTZInstructionBuilder.create().address(0x001)
                    .addFIControl(FIControlEnum.IRIS_CLOSE).irisSpeed(80).buildToHex(),
                PTZInstructionBuilder.create().address(0x001)
                    .addPresetControl(PresetControlEnum.CALL_PRESET, 7).buildToHex(),
                PTZInstructionBuilder.create().address(0x001)
                    .addCruiseControl(CruiseControlEnum.ADD_CRUISE_POINT, 1, 5).buildToHex(),
                PTZInstructionBuilder.create().address(0x001)
                    .addScanControl(ScanControlEnum.START_AUTO_SCAN, 1, ScanControlEnum.ScanOperationType.START).buildToHex(),
                PTZInstructionBuilder.create().address(0x001)
                    .addAuxiliaryControl(AuxiliaryControlEnum.SWITCH_ON, 1).buildToHex(),
            };
            for (String hex : hexes) {
                assertThat(hex).hasSize(16);
                assertThat(PTZInstructionFormat.fromHexString(hex).isValid())
                    .as("hex %s 应通过 §A.3.1 8 字节格式校验", hex)
                    .isTrue();
            }
        }
    }
}
