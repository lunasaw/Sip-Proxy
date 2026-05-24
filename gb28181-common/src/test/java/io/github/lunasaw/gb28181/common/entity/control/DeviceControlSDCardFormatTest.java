package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.1.13 存储卡格式化控制命令 JAXB 往返测试。
 */
class DeviceControlSDCardFormatTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        DeviceControlSDCardFormat control = new DeviceControlSDCardFormat(CmdTypeEnum.DEVICE_CONTROL.getType(), "12345", "34020000001320000001");
        control.setFormatSDCard(1);

        String xml = control.toString();
        assertThat(xml).contains("<CmdType>DeviceControl</CmdType>");
        assertThat(xml).contains("<FormatSDCard>1</FormatSDCard>");

        DeviceControlSDCardFormat parsed = (DeviceControlSDCardFormat) XmlBean.parseObj(xml, DeviceControlSDCardFormat.class);
        assertThat(parsed.getFormatSDCard()).isEqualTo(1);
        assertThat(parsed.getDeviceId()).isEqualTo("34020000001320000001");
    }
}
