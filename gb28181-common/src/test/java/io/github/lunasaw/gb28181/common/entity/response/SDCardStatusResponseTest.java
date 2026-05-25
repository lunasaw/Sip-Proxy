package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.6.16 存储卡状态查询应答 JAXB 往返测试。
 */
class SDCardStatusResponseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        SDCardStatusResponse response = new SDCardStatusResponse("12345", "34020000001320000001");
        response.setSumNum(2);

        SDCardStatusResponse.SDCardItem itemA = new SDCardStatusResponse.SDCardItem(1, "SD1", "ok", null, 131072, 65536);
        SDCardStatusResponse.SDCardItem itemB = new SDCardStatusResponse.SDCardItem(2, "SD2", "formatting", 50, 131072, 0);
        response.setSdCardStatusInfo(new SDCardStatusResponse.SDCardStatusInfo(2, Arrays.asList(itemA, itemB)));

        String xml = response.toString();
        assertThat(xml).contains("<CmdType>SDCardStatus</CmdType>");
        assertThat(xml).contains("<SumNum>2</SumNum>");
        assertThat(xml).contains("Num=\"2\"");
        assertThat(xml).contains("<HddName>SD1</HddName>");
        assertThat(xml).contains("<Status>ok</Status>");
        assertThat(xml).contains("<FormatProgress>50</FormatProgress>");

        SDCardStatusResponse parsed = (SDCardStatusResponse) XmlBean.parseObj(xml, SDCardStatusResponse.class);
        assertThat(parsed.getCmdType()).isEqualTo("SDCardStatus");
        assertThat(parsed.getSumNum()).isEqualTo(2);
        assertThat(parsed.getSdCardStatusInfo()).isNotNull();
        assertThat(parsed.getSdCardStatusInfo().getNum()).isEqualTo(2);
        assertThat(parsed.getSdCardStatusInfo().getItems()).hasSize(2);
        assertThat(parsed.getSdCardStatusInfo().getItems().get(0).getHddName()).isEqualTo("SD1");
        assertThat(parsed.getSdCardStatusInfo().getItems().get(1).getStatus()).isEqualTo("formatting");
        assertThat(parsed.getSdCardStatusInfo().getItems().get(1).getFormatProgress()).isEqualTo(50);
    }
}
