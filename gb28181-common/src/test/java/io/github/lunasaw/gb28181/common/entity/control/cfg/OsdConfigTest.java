package io.github.lunasaw.gb28181.common.entity.control.cfg;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.2.11 OSD 配置 JAXB 往返测试。
 */
class OsdConfigTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        OsdConfig cfg = new OsdConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), "12345", "34020000001320000001");
        cfg.setOsdConfig(new OsdConfig.OsdInfo(1920, 1080, 100, 50, 1, 0, 1));

        String xml = cfg.toString();
        assertThat(xml).contains("<CmdType>DeviceConfig</CmdType>");
        assertThat(xml).contains("<OSDConfig>");
        assertThat(xml).contains("<Length>1920</Length>");
        assertThat(xml).contains("<TimeX>100</TimeX>");
        assertThat(xml).contains("<TimeEnable>1</TimeEnable>");

        OsdConfig parsed = (OsdConfig) XmlBean.parseObj(xml, OsdConfig.class);
        assertThat(parsed.getOsdConfig().getLength()).isEqualTo(1920);
        assertThat(parsed.getOsdConfig().getTimeX()).isEqualTo(100);
        assertThat(parsed.getOsdConfig().getTimeEnable()).isEqualTo(1);
        assertThat(parsed.getOsdConfig().getTextEnable()).isEqualTo(1);
    }
}
