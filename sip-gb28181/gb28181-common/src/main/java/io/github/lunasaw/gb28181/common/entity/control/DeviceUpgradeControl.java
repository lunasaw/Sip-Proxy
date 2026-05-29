package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.3.1.12 设备软件升级控制命令
 *
 * <pre>
 * &lt;Control&gt;
 *   &lt;CmdType&gt;DeviceControl&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;DeviceUpgrade&gt;
 *     &lt;Firmware&gt;V1.0.0&lt;/Firmware&gt;
 *     &lt;FileURL&gt;http://example.com/upgrade.bin&lt;/FileURL&gt;
 *     &lt;Manufacturer&gt;Manufacturer&lt;/Manufacturer&gt;
 *     &lt;SessionID&gt;32-128 chars&lt;/SessionID&gt;
 *   &lt;/DeviceUpgrade&gt;
 * &lt;/Control&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceUpgradeControl extends DeviceControlBase {

    @XmlElement(name = "DeviceUpgrade")
    private DeviceUpgrade deviceUpgrade;

    public DeviceUpgradeControl(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("DeviceUpgrade");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "DeviceUpgrade")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DeviceUpgrade {

        @XmlElement(name = "Firmware")
        private String firmware;

        @XmlElement(name = "FileURL")
        private String fileURL;

        @XmlElement(name = "Manufacturer")
        private String manufacturer;

        @XmlElement(name = "SessionID")
        private String sessionId;
    }
}
