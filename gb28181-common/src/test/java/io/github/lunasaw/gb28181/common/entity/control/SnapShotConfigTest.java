package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.2.12 图像抓拍配置命令 JAXB 往返测试。
 */
class SnapShotConfigTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        SnapShotConfig config = new SnapShotConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), "12345", "34020000001320000001");
        config.setSnapShotConfig(new SnapShotConfig.SnapShotInfo(
                3, 2, "http://example.com/upload",
                "abcdef0123456789abcdef0123456789ab"));

        String xml = config.toString();
        assertThat(xml).contains("<CmdType>DeviceConfig</CmdType>");
        assertThat(xml).contains("<SnapShotConfig>");
        assertThat(xml).contains("<SnapNum>3</SnapNum>");
        assertThat(xml).contains("<Interval>2</Interval>");
        assertThat(xml).contains("<UploadURL>http://example.com/upload</UploadURL>");
        assertThat(xml).contains("<SessionID>abcdef0123456789abcdef0123456789ab</SessionID>");

        SnapShotConfig parsed = (SnapShotConfig) XmlBean.parseObj(xml, SnapShotConfig.class);
        assertThat(parsed.getCmdType()).isEqualTo("DeviceConfig");
        assertThat(parsed.getSn()).isEqualTo("12345");
        assertThat(parsed.getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(parsed.getSnapShotConfig()).isNotNull();
        assertThat(parsed.getSnapShotConfig().getSnapNum()).isEqualTo(3);
        assertThat(parsed.getSnapShotConfig().getInterval()).isEqualTo(2);
        assertThat(parsed.getSnapShotConfig().getUploadURL()).isEqualTo("http://example.com/upload");
        assertThat(parsed.getSnapShotConfig().getSessionId()).isEqualTo("abcdef0123456789abcdef0123456789ab");
    }
}
