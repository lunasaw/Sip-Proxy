package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.6.12 看守位信息查询应答 JAXB 往返测试。
 */
class HomePositionResponseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        HomePositionResponse response = new HomePositionResponse("12345", "34020000001320000001");
        response.setHomePosition(new HomePositionResponse.HomePositionInfo(1, 60, 5));

        String xml = response.toString();
        assertThat(xml).contains("<CmdType>HomePositionQuery</CmdType>");
        assertThat(xml).contains("<Enabled>1</Enabled>");
        assertThat(xml).contains("<ResetTime>60</ResetTime>");
        assertThat(xml).contains("<PresetIndex>5</PresetIndex>");

        HomePositionResponse parsed = (HomePositionResponse) XmlBean.parseObj(xml, HomePositionResponse.class);
        assertThat(parsed.getCmdType()).isEqualTo("HomePositionQuery");
        assertThat(parsed.getHomePosition()).isNotNull();
        assertThat(parsed.getHomePosition().getEnabled()).isEqualTo(1);
        assertThat(parsed.getHomePosition().getResetTime()).isEqualTo(60);
        assertThat(parsed.getHomePosition().getPresetIndex()).isEqualTo(5);
    }
}
