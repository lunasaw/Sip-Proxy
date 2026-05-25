package io.github.lunasaw.gb28181.common.entity.control.instruction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GBT-28181-2022 §A.3.1 8 字节指令格式测试。
 *
 * <p>对照表 A.3 校验：字节 1=A5H 固定首字节、字节 2 组合码 1（高 4 位版本=0H + 低 4 位校验位）、
 * 字节 3 地址低 8 位、字节 4 指令码、字节 5/6 数据 1/2、字节 7 组合码 2（高 4 位数据 3 + 低 4 位地址高 4 位）、
 * 字节 8 校验码 = (字节 1+...+字节 7) % 256。
 *
 * @author luna
 */
class PTZInstructionFormatTest {

    @Test
    @DisplayName("§A.3.1 字节 1 固定为 A5H")
    void headerShouldBeA5() {
        PTZInstructionFormat instr = new PTZInstructionFormat(0x001, (byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
        assertThat(instr.getHeader()).isEqualTo((byte) 0xA5);
    }

    @Test
    @DisplayName("§A.3.1 字节 2 组合码 1：版本=0H，校验位=(0xA + 0x5 + 0x0) % 16 = 0xF，所以组合码 1 固定 0x0F")
    void combinationCode1ShouldBe0F() {
        PTZInstructionFormat instr = new PTZInstructionFormat(0x001, (byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
        assertThat(instr.getCombinationCode1()).isEqualTo((byte) 0x0F);
    }

    @Test
    @DisplayName("§A.3.1 字节 3 = 地址低 8 位（地址 0x123 → 字节 3=0x23）")
    void addressLowByte() {
        PTZInstructionFormat instr = new PTZInstructionFormat(0x123, (byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
        assertThat(instr.getAddressLow()).isEqualTo((byte) 0x23);
    }

    @Test
    @DisplayName("§A.3.1 字节 7 = 数据 3<<4 | 地址高 4 位（地址 0x123, data3=0xA → 字节 7=0xA1）")
    void combinationCode2() {
        PTZInstructionFormat instr = new PTZInstructionFormat(0x123, (byte) 0x00, (byte) 0, (byte) 0, (byte) 0xA);
        assertThat(instr.getCombinationCode2()).isEqualTo((byte) 0xA1);
        assertThat(instr.getFullAddress()).isEqualTo(0x123);
        assertThat(instr.getData3()).isEqualTo((byte) 0xA);
    }

    @Test
    @DisplayName("§A.3.1 字节 8 校验码 = 前 7 字节算术和 mod 256")
    void checksumByte() {
        // 全 0 数据 + 地址 0x001：字节 1+2+3+4+5+6+7 = 0xA5+0x0F+0x01+0x00+0x00+0x00+0x00 = 0xB5
        PTZInstructionFormat instr = new PTZInstructionFormat(0x001, (byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
        assertThat(instr.getChecksum()).isEqualTo((byte) 0xB5);
        assertThat(instr.isValid()).isTrue();
    }

    @Test
    @DisplayName("toByteArray / fromByteArray 8 字节往返")
    void byteArrayRoundtrip() {
        PTZInstructionFormat src = new PTZInstructionFormat(0x123, (byte) 0x82, (byte) 0x00, (byte) 0x05, (byte) 0);
        byte[] bytes = src.toByteArray();
        assertThat(bytes).hasSize(8);
        PTZInstructionFormat parsed = PTZInstructionFormat.fromByteArray(bytes);
        assertThat(parsed.getHeader()).isEqualTo(src.getHeader());
        assertThat(parsed.getInstructionCode()).isEqualTo(src.getInstructionCode());
        assertThat(parsed.getData2()).isEqualTo(src.getData2());
        assertThat(parsed.getFullAddress()).isEqualTo(0x123);
        assertThat(parsed.isValid()).isTrue();
    }

    @Test
    @DisplayName("toHexString / fromHexString 16 字符 hex 串往返")
    void hexStringRoundtrip() {
        PTZInstructionFormat src = new PTZInstructionFormat(0x001, (byte) 0x29, (byte) 0x80, (byte) 0x80, (byte) 0x8);
        String hex = src.toHexString();
        assertThat(hex).hasSize(16);
        PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(hex);
        assertThat(parsed.toHexString()).isEqualToIgnoringCase(hex);
        assertThat(parsed.isValid()).isTrue();
    }

    @Test
    @DisplayName("fromHexString 长度非 16 抛 IllegalArgumentException")
    void fromHexStringLengthValidation() {
        assertThatThrownBy(() -> PTZInstructionFormat.fromHexString("A50F01"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("十六进制字符串长度必须为16");
    }

    @Test
    @DisplayName("fromByteArray null 或长度非 8 抛 IllegalArgumentException")
    void fromByteArrayLengthValidation() {
        assertThatThrownBy(() -> PTZInstructionFormat.fromByteArray(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PTZInstructionFormat.fromByteArray(new byte[]{0xA, 0x5}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("指令字节数组长度必须为8");
    }

    @Test
    @DisplayName("isValid 校验首字节 + 校验码")
    void isValidShouldDetectTampering() {
        PTZInstructionFormat instr = new PTZInstructionFormat(0x001, (byte) 0x82, (byte) 0x00, (byte) 0x05, (byte) 0);
        assertThat(instr.isValid()).isTrue();

        instr.setData2((byte) 0x06);  // 篡改数据，未重算校验码
        assertThat(instr.isValid()).isFalse();

        instr.recalculateChecksum();  // 重算
        assertThat(instr.isValid()).isTrue();
    }

    @Test
    @DisplayName("地址边界：0x000 / 0xFFF 都合法")
    void addressBoundary() {
        PTZInstructionFormat min = new PTZInstructionFormat(0x000, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        assertThat(min.getFullAddress()).isEqualTo(0x000);

        PTZInstructionFormat max = new PTZInstructionFormat(0xFFF, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        assertThat(max.getFullAddress()).isEqualTo(0xFFF);
    }
}
