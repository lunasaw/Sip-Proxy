package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.6.15 PTZ 精确状态查询应答 JAXB 往返测试。
 */
class PTZPositionResponseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        PTZPositionResponse response = new PTZPositionResponse("12345", "34020000001320000001");
        response.setPan(180.5);
        response.setTilt(30.25);
        response.setZoom(2.0);
        response.setHorizontalFieldAngle(60.0);
        response.setVerticalFieldAngle(40.0);
        response.setMaxViewDistance(500.0);

        String xml = response.toString();
        assertThat(xml).contains("<CmdType>PTZPosition</CmdType>");
        assertThat(xml).contains("<Pan>180.5</Pan>");
        assertThat(xml).contains("<HorizontalFieldAngle>60.0</HorizontalFieldAngle>");
        assertThat(xml).contains("<MaxViewDistance>500.0</MaxViewDistance>");

        PTZPositionResponse parsed = (PTZPositionResponse) XmlBean.parseObj(xml, PTZPositionResponse.class);
        assertThat(parsed.getCmdType()).isEqualTo("PTZPosition");
        assertThat(parsed.getPan()).isEqualTo(180.5);
        assertThat(parsed.getTilt()).isEqualTo(30.25);
        assertThat(parsed.getZoom()).isEqualTo(2.0);
        assertThat(parsed.getHorizontalFieldAngle()).isEqualTo(60.0);
        assertThat(parsed.getVerticalFieldAngle()).isEqualTo(40.0);
        assertThat(parsed.getMaxViewDistance()).isEqualTo(500.0);
    }
}
