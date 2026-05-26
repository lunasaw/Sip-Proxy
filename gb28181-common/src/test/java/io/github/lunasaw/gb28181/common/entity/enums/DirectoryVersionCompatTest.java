package io.github.lunasaw.gb28181.common.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GBT-28181-2022 附录 J §J.f-g：215/216 类型语义版本校准测试。
 *
 * <p>2016 与 2022 标准对 215/216 的定义冲突：
 * <ul>
 *   <li>2016：215=虚拟组织目录，216=中心信令控制服务器</li>
 *   <li>2022：215=业务分组，216=虚拟组织</li>
 * </ul>
 */
class DirectoryVersionCompatTest {

    @Test
    void fromCode_default_shouldUse2022Semantics() {
        // 215 在 2022 标准下应该是 BUSINESS_GROUP（业务分组）
        DeviceGbType type215 = DeviceGbType.fromCode(215);
        assertThat(type215).isEqualTo(DeviceGbType.BUSINESS_GROUP);
        assertThat(type215.getDescription()).contains("业务分组");

        // 216 在 2022 标准下应该是 VIRTUAL_ORGANIZATION（虚拟组织）
        DeviceGbType type216 = DeviceGbType.fromCode(216);
        assertThat(type216).isEqualTo(DeviceGbType.VIRTUAL_ORGANIZATION);
        assertThat(type216.getDescription()).contains("虚拟组织");
    }

    @Test
    void fromCode_v2022_shouldReturnBusinessGroupAndVirtualOrganization() {
        assertThat(DeviceGbType.fromCode(215, DirectoryVersion.V_2022))
                .isEqualTo(DeviceGbType.BUSINESS_GROUP);
        assertThat(DeviceGbType.fromCode(216, DirectoryVersion.V_2022))
                .isEqualTo(DeviceGbType.VIRTUAL_ORGANIZATION);
    }

    @Test
    @SuppressWarnings("deprecation") // 显式校验 2016 兼容枚举常量仍可解析
    void fromCode_v2016_shouldReturnLegacyNames() {
        // 2016 模式下显式取旧命名
        assertThat(DeviceGbType.fromCode(215, DirectoryVersion.V_2016))
                .isEqualTo(DeviceGbType.VIRTUAL_ORGANIZATION_DIRECTORY);
        assertThat(DeviceGbType.fromCode(216, DirectoryVersion.V_2016))
                .isEqualTo(DeviceGbType.CENTER_SIGNAL_CONTROL_SERVER);
    }

    @Test
    void fromCode_nullVersion_shouldFallbackTo2022() {
        assertThat(DeviceGbType.fromCode(215, null))
                .isEqualTo(DeviceGbType.BUSINESS_GROUP);
    }

    @Test
    void fromCode_unknownCode_shouldReturnNull() {
        assertThat(DeviceGbType.fromCode(999)).isNull();
        assertThat(DeviceGbType.fromCode(999, DirectoryVersion.V_2022)).isNull();
        assertThat(DeviceGbType.fromCode(999, DirectoryVersion.V_2016)).isNull();
    }

    @Test
    void fromCode_commonTypes_shouldBeIdenticalAcrossVersions() {
        // 200/111/118/132 在两个版本下含义一致
        assertThat(DeviceGbType.fromCode(200, DirectoryVersion.V_2016))
                .isEqualTo(DeviceGbType.fromCode(200, DirectoryVersion.V_2022))
                .isEqualTo(DeviceGbType.CENTER_SERVER);
        assertThat(DeviceGbType.fromCode(132, DirectoryVersion.V_2016))
                .isEqualTo(DeviceGbType.fromCode(132, DirectoryVersion.V_2022))
                .isEqualTo(DeviceGbType.CAMERA);
    }

    @Test
    void directoryVersionParse_shouldHandleVariants() {
        assertThat(DirectoryVersion.parse("2022")).isEqualTo(DirectoryVersion.V_2022);
        assertThat(DirectoryVersion.parse("2016")).isEqualTo(DirectoryVersion.V_2016);
        assertThat(DirectoryVersion.parse("v_2016")).isEqualTo(DirectoryVersion.V_2016);
        assertThat(DirectoryVersion.parse("V_2022")).isEqualTo(DirectoryVersion.V_2022);
        // 无法识别 / null 时退化为 2022（默认）
        assertThat(DirectoryVersion.parse(null)).isEqualTo(DirectoryVersion.V_2022);
        assertThat(DirectoryVersion.parse("garbage")).isEqualTo(DirectoryVersion.V_2022);
    }
}
