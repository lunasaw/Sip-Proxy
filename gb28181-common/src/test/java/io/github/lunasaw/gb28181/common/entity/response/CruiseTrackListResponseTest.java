package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.6.13 巡航轨迹列表查询应答 JAXB 往返测试。
 */
class CruiseTrackListResponseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        CruiseTrackListResponse response = new CruiseTrackListResponse("12345", "34020000001320000001");
        response.setSumNum(2);
        response.setCruiseTrackList(new CruiseTrackListResponse.CruiseTrackList(2, Arrays.asList(
                new CruiseTrackListResponse.CruiseTrack(0, "Track-A"),
                new CruiseTrackListResponse.CruiseTrack(1, "Track-B")
        )));

        String xml = response.toString();
        assertThat(xml).contains("<CmdType>CruiseTrackListQuery</CmdType>");
        assertThat(xml).contains("Num=\"2\"");
        assertThat(xml).contains("<Number>0</Number>");
        assertThat(xml).contains("<Name>Track-A</Name>");

        CruiseTrackListResponse parsed = (CruiseTrackListResponse) XmlBean.parseObj(xml, CruiseTrackListResponse.class);
        assertThat(parsed.getSumNum()).isEqualTo(2);
        assertThat(parsed.getCruiseTrackList().getNum()).isEqualTo(2);
        assertThat(parsed.getCruiseTrackList().getTracks()).hasSize(2);
        assertThat(parsed.getCruiseTrackList().getTracks().get(0).getName()).isEqualTo("Track-A");
    }
}
