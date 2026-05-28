package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.3.1.12 设备软件升级控制命令 JAXB 往返测试。
 */
class DeviceUpgradeControlTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        DeviceUpgradeControl control = new DeviceUpgradeControl(CmdTypeEnum.DEVICE_CONTROL.getType(), "12345", "34020000001320000001");
        DeviceUpgradeControl.DeviceUpgrade upgrade = new DeviceUpgradeControl.DeviceUpgrade(
                "V1.0.0",
                "http://example.com/firmware.bin",
                "Manufacturer-X",
                "abcdef0123456789abcdef0123456789ab"
        );
        control.setDeviceUpgrade(upgrade);

        String xml = control.toString();
        assertThat(xml).contains("<CmdType>DeviceControl</CmdType>");
        assertThat(xml).contains("<DeviceUpgrade>");
        assertThat(xml).contains("<Firmware>V1.0.0</Firmware>");
        assertThat(xml).contains("<FileURL>http://example.com/firmware.bin</FileURL>");
        assertThat(xml).contains("<Manufacturer>Manufacturer-X</Manufacturer>");
        assertThat(xml).contains("<SessionID>abcdef0123456789abcdef0123456789ab</SessionID>");

        DeviceUpgradeControl parsed = (DeviceUpgradeControl) XmlBean.parseObj(xml, DeviceUpgradeControl.class);
        assertThat(parsed.getCmdType()).isEqualTo(CmdTypeEnum.DEVICE_CONTROL.getType());
        assertThat(parsed.getSn()).isEqualTo("12345");
        assertThat(parsed.getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(parsed.getDeviceUpgrade()).isNotNull();
        assertThat(parsed.getDeviceUpgrade().getFirmware()).isEqualTo("V1.0.0");
        assertThat(parsed.getDeviceUpgrade().getFileURL()).isEqualTo("http://example.com/firmware.bin");
        assertThat(parsed.getDeviceUpgrade().getManufacturer()).isEqualTo("Manufacturer-X");
        assertThat(parsed.getDeviceUpgrade().getSessionId()).isEqualTo("abcdef0123456789abcdef0123456789ab");
    }
}
