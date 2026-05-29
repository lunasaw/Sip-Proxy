package io.github.lunasaw.gb28181.common.entity.notify;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.5.9 设备软件升级结果通知 JAXB 往返测试。
 */
class UpgradeResultNotifyTest {

    @Test
    void marshalAndUnmarshal_okResult_shouldPreserveAllFields() {
        UpgradeResultNotify notify = new UpgradeResultNotify("12345", "34020000001320000001");
        notify.setSessionId("abcdef0123456789abcdef0123456789ab");
        notify.setUpgradeResult("OK");
        notify.setFirmware("V1.0.1");

        String xml = notify.toString();
        assertThat(xml).contains("<CmdType>" + CmdTypeEnum.DEVICE_UPGRADE_RESULT.getType() + "</CmdType>");
        assertThat(xml).contains("<SessionID>abcdef0123456789abcdef0123456789ab</SessionID>");
        assertThat(xml).contains("<UpgradeResult>OK</UpgradeResult>");
        assertThat(xml).contains("<Firmware>V1.0.1</Firmware>");

        UpgradeResultNotify parsed = (UpgradeResultNotify) XmlBean.parseObj(xml, UpgradeResultNotify.class);
        assertThat(parsed.getCmdType()).isEqualTo("DeviceUpgradeResult");
        assertThat(parsed.getSn()).isEqualTo("12345");
        assertThat(parsed.getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(parsed.getSessionId()).isEqualTo("abcdef0123456789abcdef0123456789ab");
        assertThat(parsed.getUpgradeResult()).isEqualTo("OK");
        assertThat(parsed.getFirmware()).isEqualTo("V1.0.1");
        assertThat(parsed.getUpgradeFailedReason()).isNull();
    }

    @Test
    void marshalAndUnmarshal_failureWithReason_shouldPreserveReason() {
        UpgradeResultNotify notify = new UpgradeResultNotify("99999", "34020000001320000002");
        notify.setSessionId("abcdef0123456789abcdef0123456789ab");
        notify.setUpgradeResult("ERROR");
        notify.setFirmware("V1.0.0");
        notify.setUpgradeFailedReason("01");

        String xml = notify.toString();
        UpgradeResultNotify parsed = (UpgradeResultNotify) XmlBean.parseObj(xml, UpgradeResultNotify.class);
        assertThat(parsed.getUpgradeResult()).isEqualTo("ERROR");
        assertThat(parsed.getUpgradeFailedReason()).isEqualTo("01");
    }
}
