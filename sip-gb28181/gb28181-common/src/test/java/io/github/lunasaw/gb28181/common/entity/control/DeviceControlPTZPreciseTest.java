package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.1.11 PTZ 精准控制命令 JAXB 往返测试。
 */
class DeviceControlPTZPreciseTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        DeviceControlPTZPrecise control = new DeviceControlPTZPrecise(CmdTypeEnum.DEVICE_CONTROL.getType(), "12345", "34020000001320000001");
        control.setPtzPreciseCtrl(new DeviceControlPTZPrecise.PTZPreciseCtrl(180.5, 30.25, 2.0));

        String xml = control.toString();
        assertThat(xml).contains("<CmdType>DeviceControl</CmdType>");
        assertThat(xml).contains("<PTZPreciseCtrl>");
        assertThat(xml).contains("<Pan>180.5</Pan>");
        assertThat(xml).contains("<Tilt>30.25</Tilt>");
        assertThat(xml).contains("<Zoom>2.0</Zoom>");

        DeviceControlPTZPrecise parsed = (DeviceControlPTZPrecise) XmlBean.parseObj(xml, DeviceControlPTZPrecise.class);
        assertThat(parsed.getPtzPreciseCtrl()).isNotNull();
        assertThat(parsed.getPtzPreciseCtrl().getPan()).isEqualTo(180.5);
        assertThat(parsed.getPtzPreciseCtrl().getTilt()).isEqualTo(30.25);
        assertThat(parsed.getPtzPreciseCtrl().getZoom()).isEqualTo(2.0);
    }
}
