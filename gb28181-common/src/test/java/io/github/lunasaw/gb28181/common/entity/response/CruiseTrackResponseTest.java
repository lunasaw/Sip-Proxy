package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.6.14 巡航轨迹查询应答 JAXB 往返测试。
 */
class CruiseTrackResponseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        CruiseTrackResponse response = new CruiseTrackResponse("12345", "34020000001320000001");
        response.setNumber(0);
        response.setName("Track-A");
        response.setSumNum(2);
        response.setCruisePointList(new CruiseTrackResponse.CruisePointList(2, Arrays.asList(
                new CruiseTrackResponse.CruisePoint(1, 5, 8),
                new CruiseTrackResponse.CruisePoint(2, 3, 8)
        )));

        String xml = response.toString();
        assertThat(xml).contains("<CmdType>CruiseTrackQuery</CmdType>");
        assertThat(xml).contains("<Number>0</Number>");
        assertThat(xml).contains("<Name>Track-A</Name>");
        assertThat(xml).contains("<PresetIndex>1</PresetIndex>");
        assertThat(xml).contains("<StayTime>5</StayTime>");
        assertThat(xml).contains("<Speed>8</Speed>");

        CruiseTrackResponse parsed = (CruiseTrackResponse) XmlBean.parseObj(xml, CruiseTrackResponse.class);
        assertThat(parsed.getNumber()).isEqualTo(0);
        assertThat(parsed.getName()).isEqualTo("Track-A");
        assertThat(parsed.getCruisePointList().getPoints()).hasSize(2);
        assertThat(parsed.getCruisePointList().getPoints().get(0).getStayTime()).isEqualTo(5);
    }
}
