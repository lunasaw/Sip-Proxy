package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.1.14 目标跟踪控制命令 JAXB 往返测试。
 */
class DeviceControlTargetTrackTest {

    @Test
    void manualTrackingWithTargetArea_shouldRoundTrip() {
        DeviceControlTargetTrack control = new DeviceControlTargetTrack(
                CmdTypeEnum.DEVICE_CONTROL.getType(), "12345", "34020000001320000001");
        control.setTargetTrack("Manual");
        control.setDeviceId2("34020000001320000002");
        control.setTargetArea(new DeviceControlTargetTrack.TargetArea(1920, 1080, 960, 540, 200, 120));

        String xml = control.toString();
        assertThat(xml).contains("<TargetTrack>Manual</TargetTrack>");
        assertThat(xml).contains("<DeviceID2>34020000001320000002</DeviceID2>");
        assertThat(xml).contains("<TargetArea>");
        assertThat(xml).contains("<Length>1920</Length>");
        assertThat(xml).contains("<MidPointX>960</MidPointX>");

        DeviceControlTargetTrack parsed = (DeviceControlTargetTrack) XmlBean.parseObj(xml, DeviceControlTargetTrack.class);
        assertThat(parsed.getTargetTrack()).isEqualTo("Manual");
        assertThat(parsed.getDeviceId2()).isEqualTo("34020000001320000002");
        assertThat(parsed.getTargetArea()).isNotNull();
        assertThat(parsed.getTargetArea().getMidPointX()).isEqualTo(960);
        assertThat(parsed.getTargetArea().getLengthY()).isEqualTo(120);
    }

    @Test
    void autoTrackingWithoutTargetArea_shouldRoundTrip() {
        DeviceControlTargetTrack control = new DeviceControlTargetTrack(
                CmdTypeEnum.DEVICE_CONTROL.getType(), "1", "34020000001320000001");
        control.setTargetTrack("Auto");

        DeviceControlTargetTrack parsed = (DeviceControlTargetTrack) XmlBean.parseObj(control.toString(), DeviceControlTargetTrack.class);
        assertThat(parsed.getTargetTrack()).isEqualTo("Auto");
        assertThat(parsed.getTargetArea()).isNull();
    }
}
